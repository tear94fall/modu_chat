package com.example.authservice.oauth.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

/**
 * 인가(액세스·리프레시·ID 토큰 묶음)를 Redis 에 JSON 으로 둔다. 인덱스 키로 토큰 값 → id 를 찾는다.
 * Spring Authorization Server 는 인메모리와 JDBC 구현만 주는데, 인메모리는 재시작 때 리프레시 토큰이 다
 * 사라지고 JDBC 는 이 서비스에 DB 가 없어서 Redis 위에 직접 만들었다. TTL 은 가장 늦은 토큰 만료.
 */
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final String AUTHZ = "oauth2:authz:";
    private static final String IDX = "oauth2:idx:";
    private static final Duration MIN_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final RegisteredClientRepository clients;
    /** StoredAuthorization POJO 용. 타입 정보 없이 평범한 JSON. */
    private final ObjectMapper plain;
    /** attributes(principal 등)와 토큰 metadata 용. Spring Security 의 allowlist 기반 타입 정보를 쓴다. */
    private final ObjectMapper secured;

    public RedisOAuth2AuthorizationService(StringRedisTemplate redis, RegisteredClientRepository clients) {
        this.redis = redis;
        this.clients = clients;
        this.plain = new ObjectMapper().findAndRegisterModules();
        ClassLoader cl = RedisOAuth2AuthorizationService.class.getClassLoader();
        this.secured = new ObjectMapper();
        this.secured.registerModules(SecurityJackson2Modules.getModules(cl));
        this.secured.registerModule(new OAuth2AuthorizationServerJackson2Module());
        this.secured.findAndRegisterModules();
    }

    @Override
    public void save(OAuth2Authorization a) {
        OAuth2Authorization previous = findById(a.getId());
        if (previous != null) {
            removeIndexes(previous);
        }
        Duration ttl = ttlOf(a);
        redis.opsForValue().set(AUTHZ + a.getId(), writePlain(toStored(a)), ttl);
        index(a.getAccessToken(), "access", a.getId(), ttl);
        index(a.getRefreshToken(), "refresh", a.getId(), ttl);
        index(a.getToken(OidcIdToken.class), "id", a.getId(), ttl);
    }

    @Override
    public void remove(OAuth2Authorization a) {
        removeIndexes(a);
        redis.delete(AUTHZ + a.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        String json = redis.opsForValue().get(AUTHZ + id);
        return json == null ? null : fromStored(read(json));
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        List<String> types;
        if (tokenType == null) {
            types = Arrays.asList("access", "refresh", "id");
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            types = List.of("refresh");
        } else if ("id_token".equals(tokenType.getValue())) {
            types = List.of("id");
        } else {
            types = List.of("access");
        }
        for (String t : types) {
            String id = redis.opsForValue().get(IDX + t + ":" + token);
            if (id != null) {
                OAuth2Authorization a = findById(id);
                if (a != null) {
                    return a;
                }
            }
        }
        return null;
    }

    private void index(OAuth2Authorization.Token<? extends OAuth2Token> t, String type, String id, Duration ttl) {
        if (t != null) {
            redis.opsForValue().set(IDX + type + ":" + t.getToken().getTokenValue(), id, ttl);
        }
    }

    private void removeIndexes(OAuth2Authorization a) {
        List<String> keys = new ArrayList<>();
        if (a.getAccessToken() != null) keys.add(IDX + "access:" + a.getAccessToken().getToken().getTokenValue());
        if (a.getRefreshToken() != null) keys.add(IDX + "refresh:" + a.getRefreshToken().getToken().getTokenValue());
        OAuth2Authorization.Token<OidcIdToken> id = a.getToken(OidcIdToken.class);
        if (id != null) keys.add(IDX + "id:" + id.getToken().getTokenValue());
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private Duration ttlOf(OAuth2Authorization a) {
        Instant latest = Instant.now().plus(MIN_TTL);
        List<OAuth2Authorization.Token<? extends OAuth2Token>> tokens = new ArrayList<>();
        tokens.add(a.getAccessToken());
        tokens.add(a.getRefreshToken());
        tokens.add(a.getToken(OidcIdToken.class));
        for (OAuth2Authorization.Token<? extends OAuth2Token> t : tokens) {
            if (t != null && t.getToken().getExpiresAt() != null && t.getToken().getExpiresAt().isAfter(latest)) {
                latest = t.getToken().getExpiresAt();
            }
        }
        return Duration.between(Instant.now(), latest);
    }

    private StoredAuthorization toStored(OAuth2Authorization a) {
        StoredAuthorization s = new StoredAuthorization();
        s.setId(a.getId());
        s.setClientId(a.getRegisteredClientId());
        s.setPrincipalName(a.getPrincipalName());
        s.setGrantType(a.getAuthorizationGrantType().getValue());
        s.setScopes(new HashSet<>(a.getAuthorizedScopes()));
        s.setAttributesJson(writeSecured(plainCopy(a.getAttributes())));
        s.setAccessToken(storedToken(a.getAccessToken()));
        s.setRefreshToken(storedToken(a.getRefreshToken()));
        s.setIdToken(storedToken(a.getToken(OidcIdToken.class)));
        return s;
    }

    private StoredAuthorization.StoredToken storedToken(OAuth2Authorization.Token<? extends OAuth2Token> t) {
        if (t == null) return null;
        StoredAuthorization.StoredToken st = new StoredAuthorization.StoredToken();
        st.setValue(t.getToken().getTokenValue());
        st.setIssuedAt(t.getToken().getIssuedAt());
        st.setExpiresAt(t.getToken().getExpiresAt());
        if (t.getToken() instanceof OAuth2AccessToken at) {
            st.setScopes(new HashSet<>(at.getScopes()));
        }
        st.setMetadataJson(writeSecured(plainCopy(t.getMetadata())));
        return st;
    }

    private OAuth2Authorization fromStored(StoredAuthorization s) {
        RegisteredClient rc = Objects.requireNonNull(clients.findById(s.getClientId()), "등록되지 않은 클라이언트: " + s.getClientId());
        OAuth2Authorization.Builder b = OAuth2Authorization.withRegisteredClient(rc)
                .id(s.getId())
                .principalName(s.getPrincipalName())
                .authorizationGrantType(new AuthorizationGrantType(s.getGrantType()))
                .authorizedScopes(s.getScopes())
                .attributes(m -> m.putAll(readMap(s.getAttributesJson())));
        if (s.getAccessToken() != null) {
            StoredAuthorization.StoredToken t = s.getAccessToken();
            b.token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, t.getValue(), t.getIssuedAt(), t.getExpiresAt(), t.getScopes()),
                    md -> md.putAll(readMap(t.getMetadataJson())));
        }
        if (s.getRefreshToken() != null) {
            StoredAuthorization.StoredToken t = s.getRefreshToken();
            b.token(new OAuth2RefreshToken(t.getValue(), t.getIssuedAt(), t.getExpiresAt()), md -> md.putAll(readMap(t.getMetadataJson())));
        }
        if (s.getIdToken() != null) {
            StoredAuthorization.StoredToken t = s.getIdToken();
            Map<String, Object> md = readMap(t.getMetadataJson());
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = (Map<String, Object>) md.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
            b.token(new OidcIdToken(t.getValue(), t.getIssuedAt(), t.getExpiresAt(), claims), m -> m.putAll(md));
        }
        return b.build();
    }

    /**
     * Map.of / Set.of / Collections.unmodifiable* 같은 컬렉션은 Security 의 Jackson allowlist 에 없어 역직렬화가
     * 거부된다. 저장 전에 HashMap / ArrayList / HashSet 으로 깊은 복사해 둔다. 그 외 객체(Authentication 등)는 그대로.
     */
    @SuppressWarnings("unchecked")
    static Object plainCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(k, plainCopy(v)));
            return out;
        }
        if (value instanceof java.util.Set<?> set) {
            java.util.Set<Object> out = new HashSet<>();
            set.forEach(v -> out.add(plainCopy(v)));
            return out;
        }
        if (value instanceof java.util.Collection<?> col) {
            List<Object> out = new ArrayList<>();
            col.forEach(v -> out.add(plainCopy(v)));
            return out;
        }
        return value;
    }

    private String writePlain(Object o) {
        try {
            return plain.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("인가 직렬화 실패", e);
        }
    }

    private String writeSecured(Object o) {
        try {
            return secured.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("인가 속성 직렬화 실패", e);
        }
    }

    private StoredAuthorization read(String json) {
        try {
            return plain.readValue(json, StoredAuthorization.class);
        } catch (Exception e) {
            throw new IllegalStateException("인가 역직렬화 실패", e);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return secured.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("인가 메타데이터 역직렬화 실패", e);
        }
    }
}
