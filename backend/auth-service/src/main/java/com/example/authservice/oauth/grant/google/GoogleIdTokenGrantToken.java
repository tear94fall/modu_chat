package com.example.authservice.oauth.grant.google;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

public class GoogleIdTokenGrantToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String idToken;
    private final Set<String> scopes;

    public GoogleIdTokenGrantToken(Authentication clientPrincipal, String idToken, Set<String> scopes, Map<String, Object> additionalParameters) {
        super(AuthorizationServerConfig.GOOGLE_ID_TOKEN, clientPrincipal, additionalParameters);
        this.idToken = idToken;
        this.scopes = scopes;
    }

    public String getIdToken() { return idToken; }
    public Set<String> getScopes() { return scopes; }
}
