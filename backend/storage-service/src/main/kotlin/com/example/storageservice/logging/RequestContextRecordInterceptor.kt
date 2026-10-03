package com.example.storageservice.logging

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.RecordInterceptor
import java.nio.charset.StandardCharsets

/**
 * Kafka 레코드 하나를 처리하는 동안 MDC 에 requestId·userId 를 넣는다. 프로듀서가 실어 보낸 `X-Request-Id` 헤더를 쓰고,
 * 없거나 모양이 이상하면 새 id 를 만든다. userId 는 `X-Auth-User-Id` 헤더가 있을 때만 넣는다. 리스너와 에러 핸들러가 끝나면 지운다.
 */
class RequestContextRecordInterceptor<K, V> : RecordInterceptor<K, V> {

    override fun intercept(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>): ConsumerRecord<K, V> {
        MDC.put(RequestContext.MDC_REQUEST_ID, RequestContext.resolve(header(record, RequestContext.REQUEST_ID_HEADER)))
        header(record, RequestContext.USER_ID_HEADER)?.let { MDC.put(RequestContext.MDC_USER_ID, it) }
        return record
    }

    override fun afterRecord(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>) {
        MDC.remove(RequestContext.MDC_REQUEST_ID)
        MDC.remove(RequestContext.MDC_USER_ID)
    }

    private fun header(record: ConsumerRecord<K, V>, name: String): String? =
        record.headers().lastHeader(name)?.value()?.let { String(it, StandardCharsets.UTF_8) }?.takeIf { it.isNotBlank() }
}
