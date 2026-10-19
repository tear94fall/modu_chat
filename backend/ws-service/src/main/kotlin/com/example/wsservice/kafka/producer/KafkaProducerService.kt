package com.example.wsservice.kafka.producer

import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.logging.KafkaPublishLog
import com.example.wsservice.logging.KafkaRequestHeaders
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/**
 * 레코드마다 MDC 의 requestId·userId 를 헤더로 실어 보낸다(KafkaRequestHeaders) — 소비 쪽 로그가 같은 id 를 갖는다.
 * 발행 결과는 `kafka.publish` 이벤트 한 줄(KafkaPublishLog — 토픽·키·본문·성공/실패). 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가
 * 비어 있으므로, 보낼 때 잡아 둔 컨텍스트(KafkaRequestHeaders.capture)를 콜백 안에서 되살려 requestId·userId 를 붙인다.
 * 토픽 이름은 config-repo 의 `modu.kafka.topics.*`(생산자·소비자가 같은 키를 본다 — 백엔드 README "Kafka 토픽·컨슈머 그룹 이름").
 */
@Service
class KafkaProducerService(
    private val kafkaTemplate: KafkaTemplate<String, ChatMessage>,
    @Value("\${modu.kafka.topics.chat-message-save}") private val saveTopic: String,
    @Value("\${modu.kafka.topics.chat-message-read}") private val readTopic: String,
    @Value("\${modu.kafka.topics.chat-message-reaction}") private val reactionTopic: String,
) {

    fun sendMessage(key: String, message: ChatMessage) = send(saveTopic, key, message)

    fun sendReactionMessage(key: String, message: ChatMessage) = send(reactionTopic, key, message)

    fun sendReadMessage(key: String, message: ChatMessage) = send(readTopic, key, message)

    private fun send(topic: String, key: String, message: ChatMessage) {
        val record = KafkaRequestHeaders.stamp(ProducerRecord(topic, key, message))
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(record).whenComplete { result, ex ->
            context.run { if (ex == null) KafkaPublishLog.ok(record, result.recordMetadata) else KafkaPublishLog.failed(record, ex) }
        }
    }
}
