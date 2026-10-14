package com.example.pointservice.api.internal

import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 이 서비스가 Feign 으로 다른 서비스의 내부·관리자 API 를 부를 때 토큰을 붙인다. 모든 Feign 클라이언트에 적용된다. */
@Configuration
class InternalApiFeignConfig {

    @Bean
    fun internalApiTokenInterceptor(@Value("\${modu.internal-api.token}") token: String): RequestInterceptor =
        RequestInterceptor { template -> template.header(InternalApiFilter.HEADER, token) }
}
