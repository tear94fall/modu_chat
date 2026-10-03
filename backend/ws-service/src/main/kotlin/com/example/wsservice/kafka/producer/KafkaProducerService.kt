package com.example.wsservice.kafka.producer

import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.logging.KafkaRequestHeaders
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/** 레코드마다 MDC 의 requestId·userId 를 헤더로 실어 보낸다(KafkaRequestHeaders) — 소비 쪽 로그가 같은 id 를 갖는다. */
@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, ChatMessage>) {

    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)

    fun sendMessage(key: String, message: ChatMessage) = send(TOPIC, key, message, "Message")

    fun sendReactionMessage(key: String, message: ChatMessage) = send(REACTION_TOPIC, key, message, "Reaction")

    fun sendReadMessage(key: String, message: ChatMessage) = send(READ_TOPIC, key, message, "Read")

    private fun send(topic: String, key: String, message: ChatMessage, label: String) {
        kafkaTemplate.send(KafkaRequestHeaders.stamp(ProducerRecord(topic, key, message))).whenComplete { result, ex ->
            if (ex == null) {
                log.info("{} sent successfully: {}", label, result.recordMetadata.topic() + " / " + message)
            } else {
                log.info("{} sent failed: {}", label, ex.message)
            }
        }
    }

    companion object {
        private const val TOPIC = "topic-chat-save"
        private const val READ_TOPIC = "topic-chat-read"
        private const val REACTION_TOPIC = "topic-chat-reaction"
    }
}
