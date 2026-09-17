package com.example.authservice.oauth.grant.admin;

import com.example.authservice.oauth.config.AuthorizationServerConfig;
import com.example.authservice.oauth.grant.GrantRequestParser;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/** POST /oauth2/token grant_type=…admin_password&client_id=modu-admin&email=…&password=… (비밀번호는 저장하지 않는다) */
public class AdminPasswordGrantConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!GrantRequestParser.isGrant(request, AuthorizationServerConfig.ADMIN_PASSWORD.getValue())) {
            return null;
        }
        Authentication client = GrantRequestParser.clientPrincipal();
        String email = GrantRequestParser.required(request, "email");
        String password = GrantRequestParser.required(request, "password");
        return new AdminPasswordGrantToken(client, email, password, GrantRequestParser.scopes(request), Map.of());
    }
}
