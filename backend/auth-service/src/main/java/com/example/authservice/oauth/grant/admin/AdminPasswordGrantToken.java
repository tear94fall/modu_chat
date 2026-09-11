package com.example.authservice.oauth.grant.admin;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

public class AdminPasswordGrantToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String email;
    private final String password;
    private final Set<String> scopes;

    public AdminPasswordGrantToken(Authentication clientPrincipal, String email, String password, Set<String> scopes, Map<String, Object> additionalParameters) {
        super(AuthorizationServerConfig.ADMIN_PASSWORD, clientPrincipal, additionalParameters);
        this.email = email;
        this.password = password;
        this.scopes = scopes;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Set<String> getScopes() { return scopes; }
}
