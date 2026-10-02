package com.example.pushservice.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

/**
 * 실행 모듈. push-api(`…pushservice.api`)와 push-application(`…pushservice.application`)을 함께 스캔한다.
 * 데이터소스는 master/replica 두 개를 직접 만들므로(RwJpaConfig, RoJpaConfig) 자동 설정은 끈다.
 */
@SpringBootApplication(
    scanBasePackages = ["com.example.pushservice"],
    exclude = [DataSourceAutoConfiguration::class],
)
class PushApiApplication

fun main(args: Array<String>) {
    runApplication<PushApiApplication>(*args)
}
