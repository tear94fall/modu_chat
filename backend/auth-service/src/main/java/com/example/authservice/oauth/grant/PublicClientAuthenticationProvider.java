package com.example.authservice.oauth.grant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/** client_id 가 등록된 공개 클라이언트인지 확인한다. */
@RequiredArgsConstructor
public class PublicClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository clients;

    @Override
    public Authentication authenticate(Authentication authentication) {
        OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(token.getClientAuthenticationMethod())) {
            return null;
        }
        RegisteredClient rc = clients.findByClientId(String.valueOf(token.getPrincipal()));
        if (rc == null || !rc.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }
        return new OAuth2ClientAuthenticationToken(rc, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
