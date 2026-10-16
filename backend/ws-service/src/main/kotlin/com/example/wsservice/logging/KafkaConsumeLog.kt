package com.example.wsservice.logging

import net.logstash.logback.argument.StructuredArguments.kv
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

/**
 * Kafka 레코드 하나를 소비한 결과 한 줄(`kafka.consume` 이벤트) — 어느 토픽·파티션·오프셋의 어떤 키·본문을 받아 얼마 만에 처리했고,
 * 리스너가 정상으로 끝났는지(ok) 예외를 던졌는지(failed, error). [KafkaPublishLog] 와 대칭이라 발행·소비를 같은 requestId 로 이어 본다.
 * 본문은 `payload` 에 JSON 문자열로(최대 [MAX_PAYLOAD] 자, 넘으면 자른다). [RequestContextRecordInterceptor] 가 MDC 를 지우기 전에 부르니 requestId·userId 가 붙는다.
 * 리스너가 예외를 안으로 삼키면 여기엔 ok 로 남는다 — 그 실패는 리스너 자신의 로그로 본다.
 */
object KafkaConsumeLog {
    const val EVENT = "kafka.consume"
    const val LOGGER = "kafka.consume"
    const val MAX_PAYLOAD = 4096

    private val log = LoggerFactory.getLogger(LOGGER)
    private val mapper = JsonMapper.builder().build()

    fun <K, V> ok(record: ConsumerRecord<K, V>, durationMs: Long) {
        log.info(
            "kafka consume ok {}[{}]@{} key={} {}ms",
            record.topic(), record.partition(), record.offset(), record.key(), durationMs,
            kv("event", EVENT), kv("outcome", "ok"), kv("topic", record.topic()), kv("partition", record.partition()),
            kv("offset", record.offset()), kv("key", record.key()?.toString()), kv("durationMs", durationMs), kv("payload", payload(record.value())),
        )
    }

    fun <K, V> failed(record: ConsumerRecord<K, V>, durationMs: Long, error: Throwable) {
        log.error(
            "kafka consume failed {}[{}]@{} key={} {}ms: {}",
            record.topic(), record.partition(), record.offset(), record.key(), durationMs, error.message,
            kv("event", EVENT), kv("outcome", "failed"), kv("topic", record.topic()), kv("partition", record.partition()),
            kv("offset", record.offset()), kv("key", record.key()?.toString()), kv("durationMs", durationMs),
            kv("error", error.toString()), kv("payload", payload(record.value())),
            error,
        )
    }

    /** 받은 값을 JSON 으로(직렬화가 안 되면 toString). 너무 길면 앞부분만 두고 잘린 길이를 붙인다. */
    fun payload(value: Any?): String {
        val json = runCatching { mapper.writeValueAsString(value) }.getOrElse { value.toString() }
        return if (json.length <= MAX_PAYLOAD) json else json.take(MAX_PAYLOAD) + "…[truncated ${json.length - MAX_PAYLOAD} chars]"
    }
}
