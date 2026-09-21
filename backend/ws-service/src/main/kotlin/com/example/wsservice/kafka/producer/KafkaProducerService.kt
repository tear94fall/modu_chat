package com.example.wsservice.kafka.producer

import com.example.wsservice.chat.dto.ChatMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, ChatMessage>) {

    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)

    fun sendMessage(key: String, message: ChatMessage) = send(TOPIC, key, message, "Message")

    fun sendReactionMessage(key: String, message: ChatMessage) = send(REACTION_TOPIC, key, message, "Reaction")

    fun sendReadMessage(key: String, message: ChatMessage) = send(READ_TOPIC, key, message, "Read")

    private fun send(topic: String, key: String, message: ChatMessage, label: String) {
        kafkaTemplate.send(topic, key, message).whenComplete { result, ex ->
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
