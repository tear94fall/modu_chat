package com.example.gatewayservice.filter;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWKS 로 서명·만료·발급자를 검증하고, 라우트가 요구하는 role 과 audience(어느 앱의 토큰인지)를 확인한다.
 * 통과하면 X-Auth-User-Id(sub) 와 X-Auth-Client-Id(aud) 를 넣어 준다.
 */
@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    public static final String USER_ID_HEADER = "X-Auth-User-Id";
    public static final String CLIENT_ID_HEADER = "X-Auth-Client-Id";

    private final ReactiveJwtDecoder jwtDecoder;

    public AuthorizationHeaderFilter(ReactiveJwtDecoder jwtDecoder) {
        super(Config.class);
        this.jwtDecoder = jwtDecoder;
    }

    /** yml 에서 `AuthorizationHeaderFilter=ROLE_ADMIN,modu-admin` 처럼 role, audience 순으로 받는다. */
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("role", "audience");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String header = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
            if (header == null || !header.startsWith("Bearer ")) {
                return onError(exchange, "No authorization header", UNAUTHORIZED);
            }
            return jwtDecoder.decode(header.substring("Bearer ".length()))
                    .flatMap(jwt -> {
                        if (config.getAudience() != null && !config.getAudience().isBlank()
                                && (jwt.getAudience() == null || !jwt.getAudience().contains(config.getAudience()))) {
                            return onError(exchange, "JWT audience mismatch: requires " + config.getAudience(), UNAUTHORIZED);
                        }
                        List<String> roles = jwt.getClaimAsStringList("roles");
                        if (roles == null || !roles.contains(config.getRole())) {
                            return onError(exchange, "JWT role mismatch: requires " + config.getRole(), UNAUTHORIZED);
                        }
                        String userId = jwt.getSubject();
                        String clientId = jwt.getAudience() == null || jwt.getAudience().isEmpty() ? "" : jwt.getAudience().get(0);
                        ServerHttpRequest.Builder mutated = exchange.getRequest().mutate().header(CLIENT_ID_HEADER, clientId);
                        if (userId != null && !userId.isBlank()) {
                            mutated.header(USER_ID_HEADER, userId);
                        }
                        return chain.filter(exchange.mutate().request(mutated.build()).build());
                    })
                    .onErrorResume(JwtException.class, e -> onError(exchange, "JWT token is not valid: " + e.getMessage(), UNAUTHORIZED));
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
        private String audience;
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
    }
}
