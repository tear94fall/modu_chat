package com.example.authservice.oauth.grant.admin;

import com.example.authservice.admin.AdminLoginService;
import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.grant.GrantSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** 백오피스 비밀번호 로그인. 검증·잠금은 기존 AdminLoginService 그대로, 토큰만 OAuth2 로 발급한다. */
@RequiredArgsConstructor
public class AdminPasswordGrantProvider implements AuthenticationProvider {

    private final AdminLoginService adminLoginService;
    private final GrantSupport grantSupport;

    @Override
    public Authentication authenticate(Authentication authentication) {
        AdminPasswordGrantToken grant = (AdminPasswordGrantToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = (OAuth2ClientAuthenticationToken) grant.getPrincipal();
        RegisteredClient rc = clientPrincipal.getRegisteredClient();
        if (rc == null || !rc.getAuthorizationGrantTypes().contains(AuthorizationServerConfig.ADMIN_PASSWORD)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        AdminLoginService.AdminMember admin;
        try {
            admin = adminLoginService.login(grant.getEmail(), grant.getPassword());
        } catch (AdminLoginService.AdminLoginException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "관리자 로그인에 실패했습니다.", null));
        }
        return grantSupport.issue(rc, clientPrincipal, AuthorizationServerConfig.ADMIN_PASSWORD, admin.userId(), admin.roles(),
                GrantSupport.resolveScopes(rc, grant.getScopes()), grant);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AdminPasswordGrantToken.class.isAssignableFrom(authentication);
    }
}
