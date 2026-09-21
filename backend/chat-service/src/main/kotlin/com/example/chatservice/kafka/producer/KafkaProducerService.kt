package com.example.chatservice.kafka.producer

import com.example.chatservice.message.entity.ChatMessage
import com.example.chatservice.message.entity.SubscribeType
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, ChatMessage>) {

    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)

    companion object {
        private const val TOPIC = "topic-chat-broadcast"
        private const val ROOM_CREATED_TOPIC = "topic-chat-room-created"
    }

    fun sendMessage(key: String, message: ChatMessage) {
        kafkaTemplate.send(TOPIC, key, message)
            .whenComplete { result, ex ->
                if (ex == null) {
                    log.info("Message sent successfully: {}", result.recordMetadata.topic() + " / " + message)
                } else {
                    log.info("Message sent failed: {}", ex.message)
                }
            }
    }

    /** 방 생성을 ws-service 에 알린다. roomId 만 실어 보내고, 나머지 필드는 클라이언트가 방 목록을 다시 조회해 채운다. */
    fun sendRoomCreatedMessage(roomId: String) {
        val message = ChatMessage(SubscribeType.ROOM_CREATED, roomId, null)
        kafkaTemplate.send(ROOM_CREATED_TOPIC, roomId, message)
            .whenComplete { result, ex ->
                if (ex == null) {
                    log.info("Room created event sent successfully: {}", result.recordMetadata.topic() + " / " + message)
                } else {
                    log.info("Room created event sent failed: {}", ex.message)
                }
            }
    }
}
