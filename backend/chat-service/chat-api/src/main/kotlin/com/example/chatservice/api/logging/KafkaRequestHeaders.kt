package com.example.chatservice.api.logging

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import java.nio.charset.StandardCharsets

/**
 * Kafka 로 내보낼 때 MDC 의 requestId·userId 를 레코드 헤더(`X-Request-Id`, `X-Auth-User-Id`)로 실어 보낸다. 없으면 안 붙인다.
 * 발행 결과 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있으므로, [capture] 로 잡아 둔 값을 콜백 안에서 [CapturedContext.run] 으로 되살린다.
 */
object KafkaRequestHeaders {

    /** 보낼 때 잡아 둔 requestId·userId. [run] 이 블록 동안만 MDC 에 넣고 끝나면 원래 값으로 되돌린다(없던 키는 지운다). */
    class CapturedContext internal constructor(private val requestId: String?, private val userId: String?) {
        fun <T> run(block: () -> T): T {
            val previousRequestId = MDC.get(RequestContext.MDC_REQUEST_ID)
            val previousUserId = MDC.get(RequestContext.MDC_USER_ID)
            put(RequestContext.MDC_REQUEST_ID, requestId)
            put(RequestContext.MDC_USER_ID, userId)
            try {
                return block()
            } finally {
                put(RequestContext.MDC_REQUEST_ID, previousRequestId)
                put(RequestContext.MDC_USER_ID, previousUserId)
            }
        }

        private fun put(key: String, value: String?) = if (value == null) MDC.remove(key) else MDC.put(key, value)
    }

    /** 지금 스레드의 requestId·userId 를 잡아 둔다. send() 직전에 부른다. */
    fun capture(): CapturedContext = CapturedContext(RequestContext.currentRequestId(), RequestContext.currentUserId())

    fun <K, V> stamp(record: ProducerRecord<K, V>): ProducerRecord<K, V> {
        RequestContext.currentRequestId()?.let { record.headers().add(RequestContext.REQUEST_ID_HEADER, it.toByteArray(StandardCharsets.UTF_8)) }
        RequestContext.currentUserId()?.let { record.headers().add(RequestContext.USER_ID_HEADER, it.toByteArray(StandardCharsets.UTF_8)) }
        return record
    }
}
