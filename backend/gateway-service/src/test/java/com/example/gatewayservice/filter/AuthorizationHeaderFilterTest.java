package com.example.gatewayservice.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 테스트 키로 서명한 RS256 토큰으로 role/audience/만료 검사를 확인한다. JWKS 대신 공개키를 직접 준다. */
class AuthorizationHeaderFilterTest {

    static final String ISSUER = "http://localhost:8000/auth-service";
    static RSAKey key;
    static ReactiveJwtDecoder decoder;

    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @BeforeAll
    static void keys() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test").generate();
        NimbusReactiveJwtDecoder d = NimbusReactiveJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(ISSUER)));
        decoder = d;
    }

    private static String token(String sub, List<String> roles, String aud, Instant exp) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(sub).issuer(ISSUER).audience(aud)
                .claim("roles", roles).issueTime(new Date()).expirationTime(Date.from(exp)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static String valid(String aud, List<String> roles) throws Exception {
        return token("user-1", roles, aud, Instant.now().plusSeconds(600));
    }

    private MockServerWebExchange exchange(String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> b = MockServerHttpRequest.get("/x");
        if (authHeader != null) b = b.header("Authorization", authHeader);
        return MockServerWebExchange.from(b.build());
    }

    private GatewayFilter filter(String role, String audience) {
        AuthorizationHeaderFilter.Config c = new AuthorizationHeaderFilter.Config();
        c.setRole(role);
        c.setAudience(audience);
        return new AuthorizationHeaderFilter(decoder).apply(c);
    }

    @Test
    void missingHeader_is401() {
        MockServerWebExchange ex = exchange(null);
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void garbageToken_is401() {
        MockServerWebExchange ex = exchange("Bearer not-a-jwt");
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void expiredToken_is401() throws Exception {
        MockServerWebExchange ex = exchange("Bearer " + token("user-1", List.of("ROLE_USER"), "modu-chat", Instant.now().minusSeconds(600)));
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void audienceMismatch_is401() throws Exception {
        MockServerWebExchange ex = exchange("Bearer " + valid("modu-commerce", List.of("ROLE_USER")));
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void roleMismatch_is401() throws Exception {
        MockServerWebExchange ex = exchange("Bearer " + valid("modu-admin", List.of("ROLE_USER")));
        filter("ROLE_ADMIN", "modu-admin").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void match_passesWithIdentityHeaders() throws Exception {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = exchange("Bearer " + valid("modu-chat", List.of("ROLE_USER")));
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        assertNull(ex.getResponse().getStatusCode());
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("user-1", captor.getValue().getRequest().getHeaders().getFirst("X-Auth-User-Id"));
        assertEquals("modu-chat", captor.getValue().getRequest().getHeaders().getFirst("X-Auth-Client-Id"));
    }

    @Test
    void forgedIdentityHeaders_areReplacedNotAppended() throws Exception {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/x")
                .header("Authorization", "Bearer " + valid("modu-chat", List.of("ROLE_USER")))
                .header("X-Auth-User-Id", "attacker").header("X-Auth-Client-Id", "modu-admin").build();
        MockServerWebExchange ex = MockServerWebExchange.from(request);
        filter("ROLE_USER", "modu-chat").filter(ex, chain).block();
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals(List.of("user-1"), captor.getValue().getRequest().getHeaders().get("X-Auth-User-Id"));
        assertEquals(List.of("modu-chat"), captor.getValue().getRequest().getHeaders().get("X-Auth-Client-Id"));
    }

    @Test
    void noAudienceConfigured_acceptsAnyClient() throws Exception {
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = exchange("Bearer " + valid("modu-commerce", List.of("ROLE_USER")));
        filter("ROLE_USER", null).filter(ex, chain).block();
        assertNull(ex.getResponse().getStatusCode());
    }

    @Test
    void defaultRole_isUser() {
        AuthorizationHeaderFilter.Config c = new AuthorizationHeaderFilter.Config();
        assertEquals("ROLE_USER", c.getRole());
        assertNull(c.getAudience());
    }
}
