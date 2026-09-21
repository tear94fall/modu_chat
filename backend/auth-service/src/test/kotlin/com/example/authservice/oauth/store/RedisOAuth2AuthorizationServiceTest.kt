package com.example.authservice.oauth.store

import java.security.Principal
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

@SpringBootTest
class RedisOAuth2AuthorizationServiceTest {

    @Autowired lateinit var service: OAuth2AuthorizationService
    @Autowired lateinit var clients: RegisteredClientRepository

    private fun sample(id: String): OAuth2Authorization {
        val chat = clients.findByClientId("modu-chat")
        val now = Instant.now()
        val principal = UsernamePasswordAuthenticationToken("user-1", null, setOf(SimpleGrantedAuthority("ROLE_USER")))
        return OAuth2Authorization.withRegisteredClient(chat)
            .id(id).principalName("user-1")
            .authorizationGrantType(AuthorizationGrantType("urn:modu:params:oauth:grant-type:google_id_token"))
            .authorizedScopes(setOf("openid", "chat"))
            .attribute(Principal::class.java.name, principal)
            .token(
                OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-$id", now, now.plusSeconds(3600), setOf("openid", "chat")),
            ) { md -> md[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = mapOf("sub" to "user-1", "roles" to listOf("ROLE_USER")) }
            .refreshToken(OAuth2RefreshToken("refresh-$id", now, now.plusSeconds(7 * 24 * 3600)))
            .build()
    }

    @Test
    fun 저장한_인가를_id_와_각_토큰으로_찾고_삭제하면_전부_사라진다() {
        service.save(sample("a1"))

        val byId = service.findById("a1")
        assertThat(byId).isNotNull
        assertThat(byId!!.principalName).isEqualTo("user-1")
        assertThat(byId.authorizedScopes).containsExactlyInAnyOrder("openid", "chat")
        assertThat(byId.accessToken.claims).containsEntry("sub", "user-1")
        assertThat(byId.getAttribute<Any>(Principal::class.java.name)).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)

        assertThat(service.findByToken("access-a1", OAuth2TokenType.ACCESS_TOKEN)!!.id).isEqualTo("a1")
        assertThat(service.findByToken("refresh-a1", OAuth2TokenType.REFRESH_TOKEN)!!.id).isEqualTo("a1")
        assertThat(service.findByToken("refresh-a1", null)!!.id).isEqualTo("a1")
        assertThat(service.findByToken("nope", null)).isNull()

        service.remove(byId)
        assertThat(service.findById("a1")).isNull()
        assertThat(service.findByToken("access-a1", OAuth2TokenType.ACCESS_TOKEN)).isNull()
        assertThat(service.findByToken("refresh-a1", OAuth2TokenType.REFRESH_TOKEN)).isNull()
    }

    @Test
    fun 같은_id_로_다시_저장하면_옛_토큰_인덱스는_정리된다() {
        service.save(sample("a2"))
        val rotated = OAuth2Authorization.from(service.findById("a2"))
            .refreshToken(OAuth2RefreshToken("refresh-a2-new", Instant.now(), Instant.now().plusSeconds(600)))
            .build()
        service.save(rotated)

        assertThat(service.findByToken("refresh-a2-new", OAuth2TokenType.REFRESH_TOKEN)).isNotNull
        assertThat(service.findByToken("refresh-a2", OAuth2TokenType.REFRESH_TOKEN)).isNull()
    }
}
