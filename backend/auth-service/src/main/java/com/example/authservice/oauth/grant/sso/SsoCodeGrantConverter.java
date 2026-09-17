package com.example.authservice.oauth.grant.sso;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.grant.GrantRequestParser;
import com.example.authservice.oauth.grant.GrantSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/** POST /oauth2/token grant_type=…sso_code&client_id=…&code=…&code_verifier=… */
public class SsoCodeGrantConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!GrantRequestParser.isGrant(request, AuthorizationServerConfig.SSO_CODE.getValue())) {
            return null;
        }
        Authentication client = GrantRequestParser.clientPrincipal();
        String code = GrantRequestParser.required(request, "code");
        String verifier = GrantRequestParser.required(request, "code_verifier");
        return new SsoCodeGrantToken(client, code, verifier, GrantRequestParser.scopes(request),
                GrantSupport.additionalParameters(request.getParameterMap()));
    }
}
