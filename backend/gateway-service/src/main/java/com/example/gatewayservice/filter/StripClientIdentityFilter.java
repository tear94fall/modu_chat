package com.example.gatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 클라이언트가 보낸 사용자 식별 헤더를 라우팅 전에 지운다. 실제 값은 JWT 를 검증한
 * AuthorizationHeaderFilter 가 넣는다. default-filters 의 RemoveRequestHeader 로는 안 된다 —
 * 기본 필터와 라우트 필터의 순번이 따로 매겨져 인증 필터가 넣은 값을 도로 지우기 때문이다.
 */
@Component
public class StripClientIdentityFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!exchange.getRequest().getHeaders().containsKey(AuthorizationHeaderFilter.USER_ID_HEADER)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(AuthorizationHeaderFilter.USER_ID_HEADER))
                .build();
        return chain.filter(exchange.mutate().request(stripped).build());
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 1; }
}
