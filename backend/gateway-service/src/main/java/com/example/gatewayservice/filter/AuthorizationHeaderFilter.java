package com.example.gatewayservice.filter;

import com.example.gatewayservice.jwt.JwtTokenProvider;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpStatus.*;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    public static final String USER_ID_HEADER = "X-Auth-User-Id";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthorizationHeaderFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** yml 에서 `AuthorizationHeaderFilter=ROLE_ADMIN` 처럼 첫 인자를 role 로 받는다. */
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("role");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (!request.getHeaders().containsKey(AUTHORIZATION)) {
                return onError(exchange, "No authorization header", UNAUTHORIZED);
            }
            String jwt = request.getHeaders().get(AUTHORIZATION).get(0).replace("Bearer ", "");
            if (!jwtTokenProvider.validateToken(jwt)) {
                return onError(exchange, "JWT token is not valid", UNAUTHORIZED);
            }
            if (!jwtTokenProvider.getAuthentication(jwt).contains(config.getRole())) {
                return onError(exchange, "JWT role mismatch: requires " + config.getRole(), UNAUTHORIZED);
            }
            String userId = jwtTokenProvider.findUserIdByJwt(jwt);
            if (userId == null || userId.isBlank()) {
                return chain.filter(exchange);
            }
            ServerHttpRequest mutated = request.mutate().header(USER_ID_HEADER, userId).build();
            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);
        return response.setComplete();
    }

    public static class Config {
        private String role = "ROLE_USER";
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
