package com.example.authservice.oauth.store;

import java.time.Instant;
import java.util.Set;
import lombok.Data;

/** Redis 에 저장하는 OAuth2Authorization 의 평면 형태. */
@Data
public class StoredAuthorization {
    private String id;
    private String clientId;
    private String principalName;
    private String grantType;
    private Set<String> scopes;
    /** OAuth2Authorization.getAttributes() 를 AS Jackson 모듈로 직렬화한 JSON */
    private String attributesJson;
    private StoredToken accessToken;
    private StoredToken refreshToken;
    private StoredToken idToken;

    @Data
    public static class StoredToken {
        private String value;
        private Instant issuedAt;
        private Instant expiresAt;
        private Set<String> scopes;
        private String metadataJson;
    }
}
