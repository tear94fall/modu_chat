package com.example.wsservice.member.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * member-service 의 내부 API. 토큰(X-Internal-Token)은 [com.example.wsservice.api.internal.InternalApiFeignConfig]
 * 의 RequestInterceptor 빈이 컨텍스트의 모든 Feign 클라이언트에 붙여 준다(ChatFeignClient/FcmFeignClient 와 같은 방식).
 */
@FeignClient("member-service")
interface MemberFeignClient {

    /** userId 를 차단한 사람들의 userId 목록(역방향 차단). */
    @GetMapping("/api-internal/member/{userId}/blocked-by")
    fun blockedBy(@PathVariable("userId") userId: String): List<String?>?
}
