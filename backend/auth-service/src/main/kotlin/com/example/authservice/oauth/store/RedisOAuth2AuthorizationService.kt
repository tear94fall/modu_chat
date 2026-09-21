package com.example.authservice.oauth.store

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module

/**
 * 인가(액세스·리프레시·ID 토큰 묶음)를 Redis 에 JSON 으로 둔다. 인덱스 키로 토큰 값 → id 를 찾는다.
 * Spring Authorization Server 는 인메모리와 JDBC 구현만 주는데, 인메모리는 재시작 때 리프레시 토큰이 다
 * 사라지고 JDBC 는 이 서비스에 DB 가 없어서 Redis 위에 직접 만들었다. TTL 은 가장 늦은 토큰 만료.
 */
class RedisOAuth2AuthorizationService(
    private val redis: StringRedisTemplate,
    private val clients: RegisteredClientRepository,
) : OAuth2AuthorizationService {

    /** StoredAuthorization POJO 용. 타입 정보 없이 평범한 JSON. */
    private val plain: ObjectMapper = ObjectMapper().findAndRegisterModules()

    /** attributes(principal 등)와 토큰 metadata 용. Spring Security 의 allowlist 기반 타입 정보를 쓴다. */
    private val secured: ObjectMapper = ObjectMapper().also {
        it.registerModules(SecurityJackson2Modules.getModules(RedisOAuth2AuthorizationService::class.java.classLoader))
        it.registerModule(OAuth2AuthorizationServerJackson2Module())
        it.findAndRegisterModules()
    }

    override fun save(a: OAuth2Authorization) {
        val previous = findById(a.id)
        if (previous != null) {
            removeIndexes(previous)
        }
        val ttl = ttlOf(a)
        redis.opsForValue().set(AUTHZ + a.id, writePlain(toStored(a)), ttl)
        index(a.accessToken, "access", a.id, ttl)
        index(a.refreshToken, "refresh", a.id, ttl)
        index(a.getToken(OidcIdToken::class.java), "id", a.id, ttl)
    }

    override fun remove(a: OAuth2Authorization) {
        removeIndexes(a)
        redis.delete(AUTHZ + a.id)
    }

    override fun findById(id: String): OAuth2Authorization? {
        val json = redis.opsForValue().get(AUTHZ + id) ?: return null
        return fromStored(read(json))
    }

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val types = when {
            tokenType == null -> listOf("access", "refresh", "id")
            OAuth2TokenType.REFRESH_TOKEN == tokenType -> listOf("refresh")
            "id_token" == tokenType.value -> listOf("id")
            else -> listOf("access")
        }
        for (t in types) {
            val id = redis.opsForValue().get("$IDX$t:$token")
            if (id != null) {
                val a = findById(id)
                if (a != null) {
                    return a
                }
            }
        }
        return null
    }

    private fun index(t: OAuth2Authorization.Token<out OAuth2Token>?, type: String, id: String, ttl: Duration) {
        if (t != null) {
            redis.opsForValue().set("$IDX$type:${t.token.tokenValue}", id, ttl)
        }
    }

    private fun removeIndexes(a: OAuth2Authorization) {
        val keys = ArrayList<String>()
        a.accessToken?.let { keys.add("${IDX}access:${it.token.tokenValue}") }
        a.refreshToken?.let { keys.add("${IDX}refresh:${it.token.tokenValue}") }
        a.getToken(OidcIdToken::class.java)?.let { keys.add("${IDX}id:${it.token.tokenValue}") }
        if (keys.isNotEmpty()) {
            redis.delete(keys)
        }
    }

    private fun ttlOf(a: OAuth2Authorization): Duration {
        var latest = Instant.now().plus(MIN_TTL)
        val tokens = listOf(a.accessToken, a.refreshToken, a.getToken(OidcIdToken::class.java))
        for (t in tokens) {
            val expiresAt = t?.token?.expiresAt
            if (expiresAt != null && expiresAt.isAfter(latest)) {
                latest = expiresAt
            }
        }
        return Duration.between(Instant.now(), latest)
    }

    private fun toStored(a: OAuth2Authorization): StoredAuthorization {
        val s = StoredAuthorization()
        s.id = a.id
        s.clientId = a.registeredClientId
        s.principalName = a.principalName
        s.grantType = a.authorizationGrantType.value
        s.scopes = HashSet(a.authorizedScopes)
        s.attributesJson = writeSecured(plainCopy(a.attributes))
        s.accessToken = storedToken(a.accessToken)
        s.refreshToken = storedToken(a.refreshToken)
        s.idToken = storedToken(a.getToken(OidcIdToken::class.java))
        return s
    }

    private fun storedToken(t: OAuth2Authorization.Token<out OAuth2Token>?): StoredAuthorization.StoredToken? {
        if (t == null) return null
        val st = StoredAuthorization.StoredToken()
        st.value = t.token.tokenValue
        st.issuedAt = t.token.issuedAt
        st.expiresAt = t.token.expiresAt
        val token = t.token
        if (token is OAuth2AccessToken) {
            st.scopes = HashSet(token.scopes)
        }
        st.metadataJson = writeSecured(plainCopy(t.metadata))
        return st
    }

    private fun fromStored(s: StoredAuthorization): OAuth2Authorization {
        val rc = requireNotNull(clients.findById(s.clientId!!)) { "등록되지 않은 클라이언트: ${s.clientId}" }
        val b = OAuth2Authorization.withRegisteredClient(rc)
            .id(s.id)
            .principalName(s.principalName)
            .authorizationGrantType(AuthorizationGrantType(s.grantType))
            .authorizedScopes(s.scopes)
            .attributes { m -> m.putAll(readMap(s.attributesJson!!)) }
        s.accessToken?.let { t ->
            b.token(
                OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, t.value, t.issuedAt, t.expiresAt, t.scopes),
            ) { md -> md.putAll(readMap(t.metadataJson!!)) }
        }
        s.refreshToken?.let { t ->
            b.token(OAuth2RefreshToken(t.value, t.issuedAt, t.expiresAt)) { md -> md.putAll(readMap(t.metadataJson!!)) }
        }
        s.idToken?.let { t ->
            val md = readMap(t.metadataJson!!)
            @Suppress("UNCHECKED_CAST")
            val claims = md[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] as Map<String, Any>?
            b.token(OidcIdToken(t.value, t.issuedAt, t.expiresAt, claims)) { m -> m.putAll(md) }
        }
        return b.build()
    }

    private fun writePlain(o: Any): String = try {
        plain.writeValueAsString(o)
    } catch (e: Exception) {
        throw IllegalStateException("인가 직렬화 실패", e)
    }

    private fun writeSecured(o: Any?): String = try {
        secured.writeValueAsString(o)
    } catch (e: Exception) {
        throw IllegalStateException("인가 속성 직렬화 실패", e)
    }

    private fun read(json: String): StoredAuthorization = try {
        plain.readValue(json, StoredAuthorization::class.java)
    } catch (e: Exception) {
        throw IllegalStateException("인가 역직렬화 실패", e)
    }

    private fun readMap(json: String): Map<String, Any> = try {
        secured.readValue(json, object : TypeReference<Map<String, Any>>() {})
    } catch (e: Exception) {
        throw IllegalStateException("인가 메타데이터 역직렬화 실패", e)
    }

    companion object {
        private const val AUTHZ = "oauth2:authz:"
        private const val IDX = "oauth2:idx:"
        private val MIN_TTL: Duration = Duration.ofMinutes(1)

        /**
         * Map.of / Set.of / Collections.unmodifiable* 같은 컬렉션은 Security 의 Jackson allowlist 에 없어 역직렬화가
         * 거부된다. 저장 전에 HashMap / ArrayList / HashSet 으로 깊은 복사해 둔다. 그 외 객체(Authentication 등)는 그대로.
         */
        internal fun plainCopy(value: Any?): Any? = when (value) {
            is Map<*, *> -> HashMap<Any?, Any?>().also { out -> value.forEach { (k, v) -> out[k] = plainCopy(v) } }
            is Set<*> -> HashSet<Any?>().also { out -> value.forEach { out.add(plainCopy(it)) } }
            is Collection<*> -> ArrayList<Any?>().also { out -> value.forEach { out.add(plainCopy(it)) } }
            else -> value
        }
    }
}
