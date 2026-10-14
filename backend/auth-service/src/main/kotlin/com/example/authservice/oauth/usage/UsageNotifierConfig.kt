package com.example.authservice.oauth.usage

import com.example.authservice.member.client.MemberUsageFeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UsageNotifierConfig {

    /** 전용 스레드 풀을 쓴다. Executor 빈을 따로 등록하면 스프링 기본 applicationTaskExecutor 가 빠지므로 빈으로 내놓지 않는다. */
    @Bean
    fun memberUsageNotifier(client: MemberUsageFeignClient): MemberUsageNotifier = MemberUsageNotifier(client)
}
