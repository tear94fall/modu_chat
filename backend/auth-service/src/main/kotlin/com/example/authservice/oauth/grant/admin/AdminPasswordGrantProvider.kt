package com.example.authservice.oauth.grant.admin

import com.example.authservice.admin.AdminLoginService
import com.example.authservice.oauth.config.AuthorizationServerConfig
import com.example.authservice.oauth.grant.GrantSupport
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken

/** 백오피스 비밀번호 로그인. 검증·잠금은 기존 AdminLoginService 그대로, 토큰만 OAuth2 로 발급한다. */
class AdminPasswordGrantProvider(
    private val adminLoginService: AdminLoginService,
    private val grantSupport: GrantSupport,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val grant = authentication as AdminPasswordGrantToken
        val clientPrincipal = grant.principal as OAuth2ClientAuthenticationToken
        val rc = clientPrincipal.registeredClient
        if (rc == null || !rc.authorizationGrantTypes.contains(AuthorizationServerConfig.ADMIN_PASSWORD)) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }
        val admin = try {
            adminLoginService.login(grant.email, grant.password)
        } catch (e: AdminLoginService.AdminLoginException) {
            throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "관리자 로그인에 실패했습니다.", null))
        }
        return grantSupport.issue(
            rc, clientPrincipal, AuthorizationServerConfig.ADMIN_PASSWORD, admin.userId, admin.roles,
            GrantSupport.resolveScopes(rc, grant.scopes), grant,
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        AdminPasswordGrantToken::class.java.isAssignableFrom(authentication)
}
