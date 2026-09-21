package com.example.profileservice.kafka.producer

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, Any>) {

    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)

    fun sendMessage(key: String?, message: Any) {
        kafkaTemplate.send(ProducerRecord(TOPIC, key, message)).whenComplete { result, ex ->
            if (ex == null) {
                log.info("Message sent successfully: {}", result.recordMetadata.topic() + " / " + message)
            } else {
                log.info("Message sent failed: {}", ex.message)
            }
        }
    }

    companion object {
        private const val TOPIC = "topic-storage-rollback"
    }
}
