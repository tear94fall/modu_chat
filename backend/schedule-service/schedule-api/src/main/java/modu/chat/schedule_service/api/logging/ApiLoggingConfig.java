package modu.chat.schedule_service.api.logging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 이 서비스는 @ConfigurationPropertiesScan 을 쓰지 않아 {@link ApiLoggingProperties} 를 여기서 등록한다. */
@Configuration
@EnableConfigurationProperties(ApiLoggingProperties.class)
public class ApiLoggingConfig {
}
