package com.example.chatservice.application.usecase

import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.message.ChatMessage
import org.springframework.stereotype.Component

/**
 * topic-chat-save 로 들어온 메시지를 topic-chat-broadcast 로 넘긴다. 받은 객체를 그대로 다시 보낸다 —
 * ws-service 가 실어 보낸 excludeUserIds(1:1 차단 제외 대상)까지 손대지 않고 전달해야
 * 다른 ws 인스턴스도 같은 판단을 할 수 있다. key 는 방 id 다(한 방의 메시지 순서 유지).
 */
@Component
class ChatRelayUseCase(private val chatEventPublisher: ChatEventPublisher) {

    fun relay(message: ChatMessage) {
        chatEventPublisher.broadcast(message.roomId!!, message)
    }
}
