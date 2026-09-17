package com.example.authservice.oauth.grant.google;

import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.GoogleAccountDto;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.google.GoogleAccount;
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService;
import com.example.authservice.oauth.grant.GrantSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** 구글 ID 토큰 → 회원 찾기/만들기 → 모두 토큰. 누가 이 사람인지 증명하는 것은 구글 서명뿐이다. */
@RequiredArgsConstructor
public class GoogleIdTokenGrantProvider implements AuthenticationProvider {

    private final GoogleIdTokenVerifierService verifier;
    private final MemberFeignClient members;
    private final GrantSupport grantSupport;

    @Override
    public Authentication authenticate(Authentication authentication) {
        GoogleIdTokenGrantToken grant = (GoogleIdTokenGrantToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = (OAuth2ClientAuthenticationToken) grant.getPrincipal();
        RegisteredClient rc = clientPrincipal.getRegisteredClient();
        if (rc == null || !rc.getAuthorizationGrantTypes().contains(AuthorizationServerConfig.GOOGLE_ID_TOKEN)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        GoogleAccount account = verifier.verify(grant.getIdToken());
        MemberDto member = members.googleMember(new GoogleAccountDto(account.sub(), account.email(), account.name(), account.picture()));
        return grantSupport.issue(rc, clientPrincipal, AuthorizationServerConfig.GOOGLE_ID_TOKEN, member.getUserId(),
                rolesOf(member), GrantSupport.resolveScopes(rc, grant.getScopes()), grant);
    }

    static List<String> rolesOf(MemberDto member) {
        return List.of(member.getRole() == Role.ROLE_ADMIN ? "ROLE_ADMIN" : "ROLE_USER");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GoogleIdTokenGrantToken.class.isAssignableFrom(authentication);
    }
}
