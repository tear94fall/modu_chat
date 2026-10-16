package com.example.chatstoreservice.logging

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.RecordInterceptor
import java.nio.charset.StandardCharsets

/**
 * Kafka 레코드 하나를 처리하는 동안 MDC 에 requestId·userId 를 넣는다. 프로듀서가 실어 보낸 `X-Request-Id` 헤더를 쓰고,
 * 없거나 모양이 이상하면 새 id 를 만든다. userId 는 `X-Auth-User-Id` 헤더가 있을 때만 넣는다. 리스너와 에러 핸들러가 끝나면 지운다.
 *
 * 리스너가 끝나면(success/failure) 그 레코드의 소비 결과를 `kafka.consume` 이벤트로 남긴다([KafkaConsumeLog]) — MDC 를 지우기 전이라 requestId 가 붙는다.
 * 레코드는 컨슈머 스레드 하나가 intercept → 리스너 → success/failure → afterRecord 순서로 처리하니 시작 시각은 스레드 로컬에 둔다.
 */
class RequestContextRecordInterceptor<K : Any, V : Any> : RecordInterceptor<K, V> {

    private val started = ThreadLocal<Long>()

    override fun intercept(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>): ConsumerRecord<K, V> {
        MDC.put(RequestContext.MDC_REQUEST_ID, RequestContext.resolve(header(record, RequestContext.REQUEST_ID_HEADER)))
        header(record, RequestContext.USER_ID_HEADER)?.let { MDC.put(RequestContext.MDC_USER_ID, it) }
        started.set(System.nanoTime())
        return record
    }

    override fun success(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>) {
        KafkaConsumeLog.ok(record, elapsedMs())
    }

    override fun failure(record: ConsumerRecord<K, V>, exception: Exception, consumer: Consumer<K, V>) {
        KafkaConsumeLog.failed(record, elapsedMs(), exception)
    }

    override fun afterRecord(record: ConsumerRecord<K, V>, consumer: Consumer<K, V>) {
        started.remove()
        MDC.remove(RequestContext.MDC_REQUEST_ID)
        MDC.remove(RequestContext.MDC_USER_ID)
    }

    private fun elapsedMs(): Long = started.get()?.let { (System.nanoTime() - it) / 1_000_000 } ?: 0L

    private fun header(record: ConsumerRecord<K, V>, name: String): String? =
        record.headers().lastHeader(name)?.value()?.let { String(it, StandardCharsets.UTF_8) }?.takeIf { it.isNotBlank() }
}
