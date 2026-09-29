package modu.chat.schedule_service.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 실행 모듈. schedule-api(…schedule_service.api)와 schedule-application(…schedule_service.application)을 함께 스캔한다.
 * 데이터소스는 master/replica 두 개를 직접 만들므로(RwJpaConfig, RoJpaConfig) 자동 설정은 끈다.
 */
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(
		scanBasePackages = "modu.chat.schedule_service",
		exclude = DataSourceAutoConfiguration.class)
public class ScheduleApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScheduleApiApplication.class, args);
	}

}
