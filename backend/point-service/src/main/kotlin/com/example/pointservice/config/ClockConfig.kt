package com.example.pointservice.config

import java.time.Clock
import java.time.ZoneId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 하루 상한(출석 등)의 "하루" 는 한국 시간 기준이다. 테스트는 이 빈을 고정 시계로 바꾼다. */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))
}
