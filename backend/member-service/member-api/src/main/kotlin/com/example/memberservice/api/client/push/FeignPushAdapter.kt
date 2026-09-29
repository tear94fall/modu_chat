package com.example.memberservice.api.client.push

import com.example.memberservice.application.port.PushPort
import org.springframework.stereotype.Component

/** [PushPort] 의 Feign 구현. 실패는 삼키지 않는다 — 삼킬지는 유스케이스가 정한다. */
@Component
class FeignPushAdapter(private val pushFeignClient: PushFeignClient) : PushPort {

    override fun deleteToken(userId: String) {
        pushFeignClient.deleteToken(userId)
    }

    override fun broadcast(title: String?, body: String?, data: Map<String, String>?) {
        pushFeignClient.broadcast(PushMessageDto(title, body, data, null))
    }
}
