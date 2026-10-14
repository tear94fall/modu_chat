package modu.chat.schedule_service.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc 이 GET /v3/api-docs 로 내보내는 OpenAPI 문서의 머리말.
 * 서비스 포트(도커 네트워크 안)에서만 열리고, 밖에서는 게이트웨이가 시스템 콘솔 권한으로 모아 보여 준다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi(@Value("${spring.application.name:schedule-service}") String name) {
        return new OpenAPI().info(new Info()
                .title(name)
                .version("v1")
                .description("예약 작업(스케줄)을 등록해 정해진 시각에 다른 서비스를 호출하는 스케줄 서비스입니다."));
    }
}
