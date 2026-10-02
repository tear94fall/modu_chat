package com.example.profileservice.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

/**
 * 실행 모듈. profile-api(`…profileservice.api`)와 profile-application(`…profileservice.application`)을 함께 스캔한다.
 * 데이터소스는 master/replica 두 개를 직접 만들므로(RwJpaConfig, RoJpaConfig) 자동 설정은 끈다.
 */
@SpringBootApplication(
    scanBasePackages = ["com.example.profileservice"],
    exclude = [DataSourceAutoConfiguration::class],
)
@EnableFeignClients
class ProfileApiApplication

fun main(args: Array<String>) {
    runApplication<ProfileApiApplication>(*args)
}
