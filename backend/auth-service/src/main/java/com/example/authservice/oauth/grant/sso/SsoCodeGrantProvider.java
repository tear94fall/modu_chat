package com.example.authservice.oauth.grant.sso;

import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.grant.GrantSupport;
import com.example.authservice.oauth.sso.SsoCode;
import com.example.authservice.oauth.sso.SsoCodeStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** 채팅 앱이 발급한 1회용 코드 + PKCE verifier → 커머스 앱 토큰. */
@RequiredArgsConstructor
public class SsoCodeGrantProvider implements AuthenticationProvider {

    private final SsoCodeStore store;
    private final MemberFeignClient members;
    private final GrantSupport grantSupport;

    @Override
    public Authentication authenticate(Authentication authentication) {
        SsoCodeGrantToken grant = (SsoCodeGrantToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = (OAuth2ClientAuthenticationToken) grant.getPrincipal();
        RegisteredClient rc = clientPrincipal.getRegisteredClient();
        if (rc == null || !rc.getAuthorizationGrantTypes().contains(AuthorizationServerConfig.SSO_CODE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        SsoCode sso = store.consume(grant.getCode()).orElseThrow(() -> invalid("코드가 없거나 만료됐습니다."));
        if (!sso.targetClientId().equals(rc.getClientId())) {
            throw invalid("이 앱을 위해 발급된 코드가 아닙니다.");
        }
        if (!s256(grant.getCodeVerifier()).equals(sso.codeChallenge())) {
            throw invalid("code_verifier 가 일치하지 않습니다.");
        }
        MemberDto member = members.getMember(sso.sub());
        List<String> roles = List.of(member != null && member.getRole() == Role.ROLE_ADMIN ? "ROLE_ADMIN" : "ROLE_USER");
        return grantSupport.issue(rc, clientPrincipal, AuthorizationServerConfig.SSO_CODE, sso.sub(), roles,
                GrantSupport.resolveScopes(rc, grant.getScopes()), grant);
    }

    static String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static OAuth2AuthenticationException invalid(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, message, null));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SsoCodeGrantToken.class.isAssignableFrom(authentication);
    }
}
