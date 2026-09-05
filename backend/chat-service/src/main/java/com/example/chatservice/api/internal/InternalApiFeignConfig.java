package com.example.chatservice.api.internal;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이 서비스가 Feign 으로 다른 서비스의 /api-internal/** 을 부를 때 토큰을 붙인다.
 * 컨텍스트의 RequestInterceptor 빈은 모든 Feign 클라이언트에 적용된다.
 */
@Configuration
public class InternalApiFeignConfig {

    @Bean
    public RequestInterceptor internalApiTokenInterceptor(@Value("${modu.internal-api.token}") String token) {
        return template -> template.header(InternalApiFilter.HEADER, token);
    }
}
