package com.example.authservice.member.client

import com.example.authservice.member.dto.UsageRequest
import feign.Request
import java.util.concurrent.TimeUnit
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/**
 * 토큰 발급 뒤 member-service 에 이용 기록을 남긴다. 로그인 흐름의 [MemberFeignClient] 와 시간 제한을 따로 두려고
 * 클라이언트를 나눴다(contextId). X-Internal-Token 은 InternalApiFeignConfig 의 인터셉터가 붙인다.
 */
@FeignClient(name = "member-service", contextId = "memberUsageClient", configuration = [MemberUsageFeignConfig::class])
interface MemberUsageFeignClient {

    @PostMapping("/api-internal/member/usage")
    fun recordUsage(@RequestBody request: UsageRequest)
}

/** 이 클라이언트만의 설정(@Configuration 을 붙이지 않는다 — 붙이면 모든 Feign 클라이언트에 퍼진다). 연결·응답 각 2초. */
class MemberUsageFeignConfig {

    @Bean
    fun memberUsageRequestOptions(): Request.Options =
        Request.Options(2, TimeUnit.SECONDS, 2, TimeUnit.SECONDS, true)
}
