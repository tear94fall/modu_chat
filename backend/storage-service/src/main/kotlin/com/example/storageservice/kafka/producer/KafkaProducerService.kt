package com.example.storageservice.kafka.producer

import com.example.storageservice.logging.KafkaRequestHeaders
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, Any>) {

    private val log = LoggerFactory.getLogger(KafkaProducerService::class.java)

    fun sendMessage(key: String?, message: Any) {
        // 키가 null 일 수 있어(옛 자바와 같다) ProducerRecord 로 보낸다. MDC 의 requestId·userId 는 헤더로 같이 간다.
        kafkaTemplate.send(KafkaRequestHeaders.stamp(ProducerRecord(TOPIC, key, message))).whenComplete { result, ex ->
            if (ex == null) {
                log.info("Message sent successfully: {}", result.recordMetadata.topic() + " / " + message)
            } else {
                log.info("Message sent failed: {}", ex.message)
            }
        }
    }

    companion object {
        private const val TOPIC = "topic-member-rollback"
    }
}
