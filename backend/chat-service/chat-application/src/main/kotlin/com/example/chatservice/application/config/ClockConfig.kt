package com.example.chatservice.application.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 서버가 찍는 시각(새 방의 last_chat_time)의 시계. 컨테이너 시계는 UTC 라서 채팅 시각(chat_time)과 같은 UTC 문자열이 된다.
 * 테스트는 이 빈을 고정 시계로 바꿀 수 있다.
 */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
