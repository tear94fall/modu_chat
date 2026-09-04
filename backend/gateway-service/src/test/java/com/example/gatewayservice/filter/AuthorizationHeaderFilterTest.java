package com.example.gatewayservice.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gatewayservice.jwt.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class AuthorizationHeaderFilterTest {

    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    private MockServerWebExchange exchange(String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> b = MockServerHttpRequest.get("/x");
        if (authHeader != null) b = b.header("Authorization", authHeader);
        return MockServerWebExchange.from(b.build());
    }

    private GatewayFilter filter(String role) {
        AuthorizationHeaderFilter.Config c = new AuthorizationHeaderFilter.Config();
        c.setRole(role);
        return new AuthorizationHeaderFilter(jwt).apply(c);
    }

    @Test
    void missingHeader_is401() {
        MockServerWebExchange ex = exchange(null);
        filter("ROLE_USER").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void invalidToken_is401() {
        when(jwt.validateToken("bad")).thenReturn(false);
        MockServerWebExchange ex = exchange("Bearer bad");
        filter("ROLE_USER").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void roleMismatch_is401() {
        when(jwt.validateToken("t")).thenReturn(true);
        when(jwt.getAuthentication("t")).thenReturn(List.of("ROLE_ADMINX"));
        MockServerWebExchange ex = exchange("Bearer t");
        filter("ROLE_ADMIN").filter(ex, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void roleMatch_passes() {
        when(jwt.validateToken("t")).thenReturn(true);
        when(jwt.getAuthentication("t")).thenReturn(List.of("ROLE_ADMIN"));
        when(jwt.findUserIdByJwt("t")).thenReturn("admin");
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = exchange("Bearer t");
        filter("ROLE_ADMIN").filter(ex, chain).block();
        assertEquals(null, ex.getResponse().getStatusCode());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("admin", captor.getValue().getRequest().getHeaders().getFirst("X-Auth-User-Id"));
    }

    @Test
    void roleMatch_nullSubject_passesWithoutHeader() {
        when(jwt.validateToken("t")).thenReturn(true);
        when(jwt.getAuthentication("t")).thenReturn(List.of("ROLE_ADMIN"));
        when(jwt.findUserIdByJwt("t")).thenReturn(null);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = exchange("Bearer t");
        filter("ROLE_ADMIN").filter(ex, chain).block();
        assertEquals(null, ex.getResponse().getStatusCode());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertNull(captor.getValue().getRequest().getHeaders().getFirst("X-Auth-User-Id"));
    }

    @Test
    void forgedUserIdHeader_isReplacedNotAppended() {
        when(jwt.validateToken("t")).thenReturn(true);
        when(jwt.getAuthentication("t")).thenReturn(List.of("ROLE_ADMIN"));
        when(jwt.findUserIdByJwt("t")).thenReturn("admin");
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/x")
                .header("Authorization", "Bearer t")
                .header("X-Auth-User-Id", "attacker")
                .build();
        MockServerWebExchange ex = MockServerWebExchange.from(request);

        filter("ROLE_ADMIN").filter(ex, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        List<String> values = captor.getValue().getRequest().getHeaders().get("X-Auth-User-Id");
        assertEquals(1, values.size());
        assertEquals("admin", values.get(0));
    }

    @Test
    void defaultRole_isUser() {
        AuthorizationHeaderFilter.Config c = new AuthorizationHeaderFilter.Config();
        assertEquals("ROLE_USER", c.getRole());
    }
}
