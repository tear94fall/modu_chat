package com.example.authservice.oauth.grant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * 공개 클라이언트(비밀 없음)의 client_id 를 읽는다. 라이브러리 기본 변환기는 authorization_code+PKCE 만
 * 다루므로 커스텀 grant 와 refresh_token 은 여기서 받는다. 검증은 {@link PublicClientAuthenticationProvider}.
 */
public class PublicClientAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        if (!StringUtils.hasText(clientId)) {
            return null;
        }
        // 토큰 엔드포인트는 grant_type 이 있고, 폐기(revoke)·introspect 는 grant_type 없이 client_id 만 온다.
        String grant = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (grant != null && !grant.startsWith("urn:modu:") && !"refresh_token".equals(grant)) {
            return null;
        }
        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, null);
    }
}
