package com.example.profileservice.api.kafka

import com.example.profileservice.api.dto.ProfileDto
import com.example.profileservice.application.port.StorageRollbackPublisher
import com.example.profileservice.application.usecase.result.ProfileResult
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/** [StorageRollbackPublisher] 의 Kafka 구현. 메시지 값은 옛날과 같은 [ProfileDto] 다. */
@Component
class KafkaStorageRollbackPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) : StorageRollbackPublisher {

    private val log = LoggerFactory.getLogger(KafkaStorageRollbackPublisher::class.java)

    override fun publish(key: String?, profile: ProfileResult) {
        val message = ProfileDto(profile)
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
