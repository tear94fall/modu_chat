package com.example.pointservice.api.logging

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Feign 으로 다른 서비스를 부를 때 MDC 의 requestId 를 `X-Request-Id` 로 실어 보낸다. 없으면(요청 밖) 아무것도 안 붙인다. */
class RequestIdFeignInterceptor : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        RequestContext.currentRequestId()?.let { template.header(RequestContext.REQUEST_ID_HEADER, it) }
    }
}

/** 컨텍스트의 RequestInterceptor 빈은 모든 Feign 클라이언트에 적용된다. */
@Configuration
class RequestIdFeignConfig {

    @Bean
    fun requestIdFeignInterceptor(): RequestInterceptor = RequestIdFeignInterceptor()
}
