package com.example.chatservice.api.kafka.producer

import com.example.chatservice.api.logging.KafkaPublishLog
import com.example.chatservice.api.logging.KafkaRequestHeaders
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.message.ChatMessage
import com.example.chatservice.application.message.SubscribeType
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * [ChatEventPublisher] 의 Kafka 구현. 레코드마다 MDC 의 requestId·userId 를 헤더로 실어 보낸다(KafkaRequestHeaders).
 * 발행 결과는 `kafka.publish` 이벤트 한 줄(KafkaPublishLog — 토픽·키·본문·성공/실패). 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가
 * 비어 있으므로, 보낼 때 잡아 둔 컨텍스트(KafkaRequestHeaders.capture)를 콜백 안에서 되살려 requestId·userId 를 붙인다.
 * 토픽 이름은 config-repo 의 `modu.kafka.topics.*`(생산자·소비자가 같은 키를 본다 — 백엔드 README "Kafka 토픽·컨슈머 그룹 이름").
 */
@Component
class KafkaChatEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ChatMessage>,
    @Value("\${modu.kafka.topics.chat-message-broadcast}") private val broadcastTopic: String,
    @Value("\${modu.kafka.topics.chat-room-created}") private val roomCreatedTopic: String,
) : ChatEventPublisher {

    override fun broadcast(key: String, message: ChatMessage) = publish(ProducerRecord(broadcastTopic, key, message))

    /** 방 생성을 ws-service 에 알린다. roomId 만 실어 보내고, 나머지 필드는 클라이언트가 방 목록을 다시 조회해 채운다. */
    override fun roomCreated(roomId: String) {
        publish(ProducerRecord(roomCreatedTopic, roomId, ChatMessage(SubscribeType.ROOM_CREATED, roomId, null)))
    }

    private fun publish(record: ProducerRecord<String, ChatMessage>) {
        val stamped = KafkaRequestHeaders.stamp(record)
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(stamped).whenComplete { result, ex ->
            context.run { if (ex == null) KafkaPublishLog.ok(stamped, result.recordMetadata) else KafkaPublishLog.failed(stamped, ex) }
        }
    }
}
