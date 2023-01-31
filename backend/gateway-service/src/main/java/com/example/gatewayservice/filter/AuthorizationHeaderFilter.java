package com.example.gatewayservice.filter;

import com.example.gatewayservice.jwt.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
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

    JwtTokenProvider jwtTokenProvider;

    public AuthorizationHeaderFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(AUTHORIZATION)) {
                return onError(exchange, "No authorization header", UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

            jwtTokenProvider.validateToken(jwt);

            if(!jwtTokenProvider.getRolesToken(jwt).contains("USER")) {
                return onError(exchange, "JWT token is not valid", UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        log.error(err);

        return response.setComplete();
    }

    public static class Config {

    }
}