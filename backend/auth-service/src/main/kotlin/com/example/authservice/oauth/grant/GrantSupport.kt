package com.example.authservice.oauth.grant

import java.security.Principal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClaimAccessor
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

/** 커스텀 grant 가 검증을 끝낸 뒤 공통으로 쓰는 토큰 발급·저장. */
class GrantSupport(
    private val tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
    private val authorizationService: OAuth2AuthorizationService,
) {

    fun issue(
        rc: RegisteredClient,
        clientPrincipal: OAuth2ClientAuthenticationToken,
        grantType: AuthorizationGrantType,
        userId: String,
        roles: List<String>,
        scopes: Set<String>,
        grant: Authentication,
    ): OAuth2AccessTokenAuthenticationToken {
        val authorities = roles.map { SimpleGrantedAuthority(it) }.toSet()
        val principal = UsernamePasswordAuthenticationToken(userId, null, authorities)

        val ctx = DefaultOAuth2TokenContext.builder()
            .registeredClient(rc)
            .principal(principal)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorizedScopes(scopes)
            .authorizationGrantType(grantType)
            .authorizationGrant(grant)
        val ab = OAuth2Authorization.withRegisteredClient(rc)
            .principalName(userId)
            .authorizationGrantType(grantType)
            .authorizedScopes(scopes)
            .attribute(Principal::class.java.name, principal)

        val generated = tokenGenerator.generate(ctx.tokenType(OAuth2TokenType.ACCESS_TOKEN).build())
            ?: throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "액세스 토큰을 만들지 못했습니다.", null))
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, generated.tokenValue,
            generated.issuedAt, generated.expiresAt, scopes,
        )
        if (generated is ClaimAccessor) {
            ab.token(accessToken) { md -> md[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = generated.claims }
        } else {
            ab.accessToken(accessToken)
        }

        var refreshToken: OAuth2RefreshToken? = null
        if (rc.authorizationGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            val r = tokenGenerator.generate(ctx.tokenType(OAuth2TokenType.REFRESH_TOKEN).build())
            if (r is OAuth2RefreshToken) {
                refreshToken = r
                ab.refreshToken(r)
            }
        }

        val additional = HashMap<String, Any>()
        if (scopes.contains(OidcScopes.OPENID)) {
            val id = tokenGenerator.generate(ctx.tokenType(OAuth2TokenType(OidcParameterNames.ID_TOKEN)).build())
            if (id is Jwt) {
                val idToken = OidcIdToken(id.tokenValue, id.issuedAt, id.expiresAt, id.claims)
                ab.token(idToken) { md -> md[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = idToken.claims }
                additional[OidcParameterNames.ID_TOKEN] = idToken.tokenValue
            }
        }

        authorizationService.save(ab.build())
        return OAuth2AccessTokenAuthenticationToken(rc, clientPrincipal, accessToken, refreshToken, additional)
    }

    companion object {
        /** 요청 scope 가 없으면 클라이언트 전체. 있으면 허용된 것만 남기고, 허용 안 된 값이 있으면 invalid_scope. */
        @JvmStatic
        fun resolveScopes(rc: RegisteredClient, requested: Set<String>?): MutableSet<String> {
            if (requested.isNullOrEmpty()) {
                return HashSet(rc.scopes)
            }
            for (s in requested) {
                if (!rc.scopes.contains(s)) {
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "허용되지 않은 scope: $s", null))
                }
            }
            return HashSet(requested)
        }

        /** 폼 파라미터에서 grant 토큰의 부가 파라미터 맵을 만든다(grant_type, client_id 제외). */
        @JvmStatic
        fun additionalParameters(params: Map<String, Array<String>>): MutableMap<String, Any> {
            val out = HashMap<String, Any>()
            params.forEach { (k, v) ->
                if (k != "grant_type" && k != "client_id" && v.isNotEmpty()) {
                    out[k] = if (v.size == 1) v[0] else v
                }
            }
            return out
        }
    }
}
