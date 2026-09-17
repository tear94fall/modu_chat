package com.example.authservice.oauth.grant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.util.StringUtils;

/** 커스텀 grant 변환기들의 공통 파싱. */
public final class GrantRequestParser {

    private GrantRequestParser() {}

    public static boolean isGrant(HttpServletRequest request, String grantType) {
        return grantType.equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));
    }

    /** 앞 단계(클라이언트 인증)가 넣어 둔 클라이언트 principal. */
    public static Authentication clientPrincipal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof OAuth2ClientAuthenticationToken t && t.isAuthenticated()) {
            return a;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    public static String required(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (!StringUtils.hasText(v) || request.getParameterValues(name).length != 1) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "파라미터가 없거나 중복입니다: " + name, null));
        }
        return v;
    }

    public static Set<String> scopes(HttpServletRequest request) {
        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
        if (!StringUtils.hasText(scope)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(scope.trim().split("\\s+")));
    }
}
