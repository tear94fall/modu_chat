package com.example.memberservice.api.client.chat

import com.example.memberservice.application.port.ChatRoomPort
import org.springframework.stereotype.Component

/** [ChatRoomPort] 의 Feign 구현. 실패는 삼키지 않는다 — 방 정리가 안 되면 탈퇴도 되지 않아야 한다. */
@Component
class FeignChatRoomAdapter(private val chatFeignClient: ChatFeignClient) : ChatRoomPort {

    override fun exitAllChatRooms(memberId: Long) {
        chatFeignClient.exitAllChatRooms(memberId)
    }
}
