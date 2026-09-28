package com.example.memberservice.config

import com.example.memberservice.usage.UsageProperties
import java.time.Clock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(UsageProperties::class)
class ClockConfig {

    /** 서비스 이용 기록 시각은 UTC 로 남긴다. 테스트는 고정 시계를 넘겨 서비스를 직접 만든다. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
