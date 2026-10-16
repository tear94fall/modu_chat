package com.example.storageservice.kafka.producer

import com.example.storageservice.logging.KafkaPublishLog
import com.example.storageservice.logging.KafkaRequestHeaders
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, Any>) {

    fun sendMessage(key: String?, message: Any) {
        // 키가 null 일 수 있어(옛 자바와 같다) ProducerRecord 로 보낸다. MDC 의 requestId·userId 는 헤더로 같이 간다.
        // Spring Kafka 4 의 제네릭이 non-null 로 표시돼 타입을 명시한다(키는 Kafka 쪽 타입이라 null 이어도 된다).
        val record: ProducerRecord<String, Any> = ProducerRecord(TOPIC, key, message)
        // 발행 결과는 kafka.publish 이벤트 한 줄(KafkaPublishLog). 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있다 —
        // 보낼 때 잡아 둔 컨텍스트를 콜백 안에서 되살려 requestId·userId 를 붙인다.
        val stamped = KafkaRequestHeaders.stamp(record)
        val context = KafkaRequestHeaders.capture()
        kafkaTemplate.send(stamped).whenComplete { result, ex ->
            context.run { if (ex == null) KafkaPublishLog.ok(stamped, result.recordMetadata) else KafkaPublishLog.failed(stamped, ex) }
        }
    }

    companion object {
        private const val TOPIC = "topic-member-rollback"
    }
}
