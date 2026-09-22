package com.example.authservice.oauth.grant.sso

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.Role
import com.example.authservice.oauth.config.AuthorizationServerConfig
import com.example.authservice.oauth.grant.GrantSupport
import com.example.authservice.oauth.sso.SsoCodeStore
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken

/** 채팅 앱이 발급한 1회용 코드 + PKCE verifier → 커머스 앱 토큰. */
class SsoCodeGrantProvider(
    private val store: SsoCodeStore,
    private val members: MemberFeignClient,
    private val grantSupport: GrantSupport,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val grant = authentication as SsoCodeGrantToken
        val clientPrincipal = grant.principal as OAuth2ClientAuthenticationToken
        val rc = clientPrincipal.registeredClient
        if (rc == null || !rc.authorizationGrantTypes.contains(AuthorizationServerConfig.SSO_CODE)) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }
        val sso = store.consume(grant.code) ?: throw invalid("코드가 없거나 만료됐습니다.")
        if (sso.targetClientId != rc.clientId) {
            throw invalid("이 앱을 위해 발급된 코드가 아닙니다.")
        }
        if (s256(grant.codeVerifier) != sso.codeChallenge) {
            throw invalid("code_verifier 가 일치하지 않습니다.")
        }
        val member = members.getMember(sso.sub)
        val roles = listOf(if (member != null && member.role == Role.ROLE_ADMIN) "ROLE_ADMIN" else "ROLE_USER")
        return grantSupport.issue(
            rc, clientPrincipal, AuthorizationServerConfig.SSO_CODE, sso.sub, roles,
            GrantSupport.resolveScopes(rc, grant.scopes), grant,
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        SsoCodeGrantToken::class.java.isAssignableFrom(authentication)

    companion object {
        internal fun s256(verifier: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII))
                return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }

        private fun invalid(message: String): OAuth2AuthenticationException =
            OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, message, null))
    }
}
