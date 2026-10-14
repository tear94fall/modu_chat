package com.example.chatstoreservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * springdoc 이 GET /v3/api-docs 로 내보내는 OpenAPI 문서의 머리말.
 * 서비스 포트(도커 네트워크 안)에서만 열리고, 밖에서는 게이트웨이가 시스템 콘솔 권한으로 모아 보여 준다.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(@Value("\${spring.application.name:chat-store-service}") name: String): OpenAPI =
        OpenAPI().info(
            Info()
                .title(name)
                .version("v1")
                .description("Kafka 로 받은 채팅 메시지를 MongoDB 에 보관하는 채팅 저장 서비스입니다."),
        )
}
