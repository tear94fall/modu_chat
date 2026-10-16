package com.example.chatservice.api.kafka.producer

import com.example.chatservice.api.logging.KafkaRequestHeaders
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.message.ChatMessage
import com.example.chatservice.application.message.SubscribeType
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * [ChatEventPublisher] 의 Kafka 구현. 레코드마다 MDC 의 requestId·userId 를 헤더로 실어 보낸다(KafkaRequestHeaders).
 * 발행 결과 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있으므로, 보낼 때 잡아 둔 컨텍스트를 콜백 로그에 되살린다.
 */
@Component
class KafkaChatEventPublisher(private val kafkaTemplate: KafkaTemplate<String, ChatMessage>) : ChatEventPublisher {

    private val log = LoggerFactory.getLogger(KafkaChatEventPublisher::class.java)

    override fun broadcast(key: String, message: ChatMessage) {
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(KafkaRequestHeaders.stamp(ProducerRecord(TOPIC, key, message)))
            .whenComplete { result, ex ->
                context.run {
                    if (ex == null) {
                        log.info("Message sent successfully: {}", result.recordMetadata.topic() + " / " + message)
                    } else {
                        log.info("Message sent failed: {}", ex.message)
                    }
                }
            }
    }

    /** 방 생성을 ws-service 에 알린다. roomId 만 실어 보내고, 나머지 필드는 클라이언트가 방 목록을 다시 조회해 채운다. */
    override fun roomCreated(roomId: String) {
        val message = ChatMessage(SubscribeType.ROOM_CREATED, roomId, null)
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(KafkaRequestHeaders.stamp(ProducerRecord(ROOM_CREATED_TOPIC, roomId, message)))
            .whenComplete { result, ex ->
                context.run {
                    if (ex == null) {
                        log.info("Room created event sent successfully: {}", result.recordMetadata.topic() + " / " + message)
                    } else {
                        log.info("Room created event sent failed: {}", ex.message)
                    }
                }
            }
    }

    companion object {
        private const val TOPIC = "topic-chat-broadcast"
        private const val ROOM_CREATED_TOPIC = "topic-chat-room-created"
    }
}
