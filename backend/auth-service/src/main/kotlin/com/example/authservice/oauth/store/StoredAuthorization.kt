package com.example.authservice.oauth.store

import java.time.Instant

/** Redis 에 저장하는 OAuth2Authorization 의 평면 형태. */
class StoredAuthorization {
    var id: String? = null
    var clientId: String? = null
    var principalName: String? = null
    var grantType: String? = null
    var scopes: Set<String>? = null

    /** OAuth2Authorization.getAttributes() 를 AS Jackson 모듈로 직렬화한 JSON */
    var attributesJson: String? = null
    var accessToken: StoredToken? = null
    var refreshToken: StoredToken? = null
    var idToken: StoredToken? = null

    class StoredToken {
        var value: String? = null
        var issuedAt: Instant? = null
        var expiresAt: Instant? = null
        var scopes: Set<String>? = null
        var metadataJson: String? = null
    }
}
