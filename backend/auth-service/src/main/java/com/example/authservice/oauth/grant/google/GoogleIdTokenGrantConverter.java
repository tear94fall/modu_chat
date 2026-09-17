package com.example.authservice.oauth.grant.google;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.grant.GrantRequestParser;
import com.example.authservice.oauth.grant.GrantSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/** POST /oauth2/token grant_type=…google_id_token&client_id=…&id_token=… */
public class GoogleIdTokenGrantConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!GrantRequestParser.isGrant(request, AuthorizationServerConfig.GOOGLE_ID_TOKEN.getValue())) {
            return null;
        }
        Authentication client = GrantRequestParser.clientPrincipal();
        String idToken = GrantRequestParser.required(request, "id_token");
        return new GoogleIdTokenGrantToken(client, idToken, GrantRequestParser.scopes(request),
                GrantSupport.additionalParameters(request.getParameterMap()));
    }
}
