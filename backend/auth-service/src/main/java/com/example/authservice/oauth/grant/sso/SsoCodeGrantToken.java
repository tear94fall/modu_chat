package com.example.authservice.oauth.grant.sso;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

public class SsoCodeGrantToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String code;
    private final String codeVerifier;
    private final Set<String> scopes;

    public SsoCodeGrantToken(Authentication clientPrincipal, String code, String codeVerifier, Set<String> scopes, Map<String, Object> additionalParameters) {
        super(AuthorizationServerConfig.SSO_CODE, clientPrincipal, additionalParameters);
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.scopes = scopes;
    }

    public String getCode() { return code; }
    public String getCodeVerifier() { return codeVerifier; }
    public Set<String> getScopes() { return scopes; }
}
