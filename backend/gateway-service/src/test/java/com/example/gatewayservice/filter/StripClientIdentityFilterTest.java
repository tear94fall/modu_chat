package com.example.gatewayservice.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class StripClientIdentityFilterTest {

    private final StripClientIdentityFilter filter = new StripClientIdentityFilter();
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @Test
    void forgedUserIdHeader_isStrippedBeforeChain() {
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = MockServerWebExchange.from(
                MockServerHttpRequest.get("/x").header("X-Auth-User-Id", "attacker").build());

        filter.filter(ex, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertFalse(captor.getValue().getRequest().getHeaders().containsKey("X-Auth-User-Id"));
    }

    @Test
    void requestWithoutHeader_passesThroughUnchanged() {
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/x").build());

        filter.filter(ex, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertTrue(captor.getValue() == ex);
        assertFalse(captor.getValue().getRequest().getHeaders().containsKey("X-Auth-User-Id"));
    }
}
