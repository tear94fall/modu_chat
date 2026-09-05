package com.example.gatewayservice.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class DotSegmentRejectFilterTest {

    private final DotSegmentRejectFilter filter = new DotSegmentRejectFilter();
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @Test
    void dotDotSegment_isDetected() {
        assertTrue(DotSegmentRejectFilter.hasDotSegment("/a/../b"));
    }

    @Test
    void encodedDotDotSegment_isDetected() {
        assertTrue(DotSegmentRejectFilter.hasDotSegment("/a/%2e%2e/b"));
    }

    @Test
    void singleDotSegment_isDetected() {
        assertTrue(DotSegmentRejectFilter.hasDotSegment("/a/./b"));
    }

    @Test
    void dotsWithinSegment_isNotDetected() {
        assertFalse(DotSegmentRejectFilter.hasDotSegment("/a/b..c"));
    }

    @Test
    void normalAdminPath_isNotDetected() {
        assertFalse(DotSegmentRejectFilter.hasDotSegment("/api-admin/member"));
    }

    @Test
    void filter_rejectsDotSegmentPathWithoutCallingChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api-public/../api-internal").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(exchange);
    }
}
