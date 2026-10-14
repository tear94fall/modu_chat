package com.example.authservice.config

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
    fun openApi(@Value("\${spring.application.name:auth-service}") name: String): OpenAPI =
        OpenAPI().info(
            Info()
                .title(name)
                .version("v1")
                .description("OAuth2 인가 서버로 앱·콘솔 로그인 토큰(JWT)과 SSO 코드를 발급하는 인증 서비스입니다."),
        )
}
