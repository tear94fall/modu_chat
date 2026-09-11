package com.example.authservice.oauth.grant;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/** 커스텀 grant 가 검증을 끝낸 뒤 공통으로 쓰는 토큰 발급·저장. */
@RequiredArgsConstructor
public class GrantSupport {

    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;

    public OAuth2AccessTokenAuthenticationToken issue(RegisteredClient rc, OAuth2ClientAuthenticationToken clientPrincipal,
                                                      AuthorizationGrantType grantType, String userId, List<String> roles,
                                                      Set<String> scopes, Authentication grant) {
        Set<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(userId, null, authorities);

        DefaultOAuth2TokenContext.Builder ctx = DefaultOAuth2TokenContext.builder()
                .registeredClient(rc)
                .principal(principal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(scopes)
                .authorizationGrantType(grantType)
                .authorizationGrant(grant);
        OAuth2Authorization.Builder ab = OAuth2Authorization.withRegisteredClient(rc)
                .principalName(userId)
                .authorizationGrantType(grantType)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), principal);

        OAuth2Token generated = tokenGenerator.generate(ctx.tokenType(OAuth2TokenType.ACCESS_TOKEN).build());
        if (generated == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "액세스 토큰을 만들지 못했습니다.", null));
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, generated.getTokenValue(),
                generated.getIssuedAt(), generated.getExpiresAt(), scopes);
        if (generated instanceof ClaimAccessor claims) {
            ab.token(accessToken, md -> md.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims.getClaims()));
        } else {
            ab.accessToken(accessToken);
        }

        OAuth2RefreshToken refreshToken = null;
        if (rc.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2Token r = tokenGenerator.generate(ctx.tokenType(OAuth2TokenType.REFRESH_TOKEN).build());
            if (r instanceof OAuth2RefreshToken rt) {
                refreshToken = rt;
                ab.refreshToken(rt);
            }
        }

        Map<String, Object> additional = new HashMap<>();
        if (scopes.contains(OidcScopes.OPENID)) {
            OAuth2Token id = tokenGenerator.generate(ctx.tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN)).build());
            if (id instanceof Jwt jwt) {
                OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
                ab.token(idToken, md -> md.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idToken.getClaims()));
                additional.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
            }
        }

        authorizationService.save(ab.build());
        return new OAuth2AccessTokenAuthenticationToken(rc, clientPrincipal, accessToken, refreshToken, additional);
    }

    /** 요청 scope 가 없으면 클라이언트 전체. 있으면 허용된 것만 남기고, 허용 안 된 값이 있으면 invalid_scope. */
    public static Set<String> resolveScopes(RegisteredClient rc, Set<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return new HashSet<>(rc.getScopes());
        }
        for (String s : requested) {
            if (!rc.getScopes().contains(s)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE, "허용되지 않은 scope: " + s, null));
            }
        }
        return new HashSet<>(requested);
    }

    /** 폼 파라미터에서 grant 토큰의 부가 파라미터 맵을 만든다(grant_type, client_id 제외). */
    public static Map<String, Object> additionalParameters(Map<String, String[]> params) {
        Map<String, Object> out = new HashMap<>();
        params.forEach((k, v) -> {
            if (!"grant_type".equals(k) && !"client_id".equals(k) && v != null && v.length > 0) {
                out.put(k, v.length == 1 ? v[0] : v);
            }
        });
        return out;
    }
}
