package com.example.gatewayservice.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * auth-service 의 JWKS(공개키)로 RS256 토큰을 검증한다. 공유 비밀키가 없으므로 게이트웨이는 토큰을 만들 수 없다.
 * Nimbus 가 JWKS 를 캐시해서 auth-service 가 잠시 내려가도 이미 받은 키로는 계속 검증된다.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${modu.oauth.jwks-uri}") String jwksUri,
                                                 @Value("${modu.oauth.issuer}") String issuer) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }
}
