package com.example.profileservice.api.kafka

import com.example.profileservice.api.dto.ProfileDto
import com.example.profileservice.api.logging.KafkaPublishLog
import com.example.profileservice.api.logging.KafkaRequestHeaders
import com.example.profileservice.application.port.StorageRollbackPublisher
import com.example.profileservice.application.usecase.result.ProfileResult
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/** [StorageRollbackPublisher] 의 Kafka 구현. 메시지 값은 옛날과 같은 [ProfileDto] 다. MDC 의 requestId·userId 는 헤더로 같이 간다. */
@Component
class KafkaStorageRollbackPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) : StorageRollbackPublisher {

    override fun publish(key: String?, profile: ProfileResult) {
        // 발행 결과는 kafka.publish 이벤트 한 줄(KafkaPublishLog). 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있다 —
        // 보낼 때 잡아 둔 컨텍스트를 콜백 안에서 되살려 requestId·userId 를 붙인다.
        val record = KafkaRequestHeaders.stamp(ProducerRecord<String, Any>(TOPIC, key, ProfileDto(profile)))
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(record).whenComplete { result, ex ->
            context.run { if (ex == null) KafkaPublishLog.ok(record, result.recordMetadata) else KafkaPublishLog.failed(record, ex) }
        }
    }

    companion object {
        private const val TOPIC = "topic-storage-rollback"
    }
}
