package com.example.profileservice.api.kafka

import com.example.profileservice.api.dto.ProfileDto
import com.example.profileservice.api.logging.KafkaRequestHeaders
import com.example.profileservice.application.port.StorageRollbackPublisher
import com.example.profileservice.application.usecase.result.ProfileResult
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/** [StorageRollbackPublisher] 의 Kafka 구현. 메시지 값은 옛날과 같은 [ProfileDto] 다. MDC 의 requestId·userId 는 헤더로 같이 간다. */
@Component
class KafkaStorageRollbackPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) : StorageRollbackPublisher {

    private val log = LoggerFactory.getLogger(KafkaStorageRollbackPublisher::class.java)

    override fun publish(key: String?, profile: ProfileResult) {
        val message = ProfileDto(profile)
        // 발행 결과 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있다 — 보낼 때 잡아 둔 컨텍스트를 콜백 로그에 되살린다.
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(KafkaRequestHeaders.stamp(ProducerRecord<String, Any>(TOPIC, key, message))).whenComplete { result, ex ->
            context.run {
                if (ex == null) {
                    log.info("Message sent successfully: {}", result.recordMetadata.topic() + " / " + message)
                } else {
                    log.info("Message sent failed: {}", ex.message)
                }
            }
        }
    }

    companion object {
        private const val TOPIC = "topic-storage-rollback"
    }
}
