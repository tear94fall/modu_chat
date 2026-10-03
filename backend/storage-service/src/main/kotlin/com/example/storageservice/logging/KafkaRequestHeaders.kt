package com.example.storageservice.logging

import org.apache.kafka.clients.producer.ProducerRecord
import java.nio.charset.StandardCharsets

/** Kafka 로 내보낼 때 MDC 의 requestId·userId 를 레코드 헤더(`X-Request-Id`, `X-Auth-User-Id`)로 실어 보낸다. 없으면 안 붙인다. */
object KafkaRequestHeaders {

    fun <K, V> stamp(record: ProducerRecord<K, V>): ProducerRecord<K, V> {
        RequestContext.currentRequestId()?.let { record.headers().add(RequestContext.REQUEST_ID_HEADER, it.toByteArray(StandardCharsets.UTF_8)) }
        RequestContext.currentUserId()?.let { record.headers().add(RequestContext.USER_ID_HEADER, it.toByteArray(StandardCharsets.UTF_8)) }
        return record
    }
}
