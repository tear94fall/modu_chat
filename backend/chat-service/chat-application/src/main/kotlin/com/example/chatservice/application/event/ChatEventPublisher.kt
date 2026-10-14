package com.example.chatservice.application.event

import com.example.chatservice.application.message.ChatMessage

/** Kafka 발행 포트. 구현은 chat-api 의 프로듀서 어댑터다. */
interface ChatEventPublisher {

    /** topic-chat-broadcast 로 보낸다. key 는 방 id 라서 한 방의 메시지는 한 파티션에서 순서가 지켜진다. */
    fun broadcast(key: String, message: ChatMessage)

    /** 방 생성을 ws-service 에 알린다(topic-chat-room-created). roomId 만 실어 보낸다. */
    fun roomCreated(roomId: String)
}
