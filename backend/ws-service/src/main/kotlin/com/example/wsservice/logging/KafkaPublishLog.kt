package com.example.wsservice.logging

import net.logstash.logback.argument.StructuredArguments.kv
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

/**
 * Kafka 발행 결과 한 줄(`kafka.publish` 이벤트) — 어느 토픽에 어떤 키·본문을 보냈고 성공(partition·offset)했는지, 실패(error)했는지.
 * http.access·api.access 처럼 `attributes.event` 로 구분해 본다. 본문은 `payload` 에 JSON 문자열로(최대 [MAX_PAYLOAD] 자, 넘으면 자른다).
 * 콜백 스레드에는 MDC 가 없으니 부르는 쪽이 [KafkaRequestHeaders.capture] 로 잡아 둔 컨텍스트 안에서 부른다 — 그래야 requestId·userId 가 붙는다.
 * 성공은 INFO, 실패는 ERROR(스택 포함) — 발행 실패는 받는 쪽이 영영 못 받는 사고라 에러로 다뤄야 한다.
 */
object KafkaPublishLog {
    const val EVENT = "kafka.publish"
    const val LOGGER = "kafka.publish"
    const val MAX_PAYLOAD = 4096

    private val log = LoggerFactory.getLogger(LOGGER)
    private val mapper = JsonMapper.builder().build()

    fun <K, V> ok(record: ProducerRecord<K, V>, metadata: RecordMetadata) {
        log.info(
            "kafka publish ok {}[{}]@{} key={}",
            record.topic(), metadata.partition(), metadata.offset(), record.key(),
            kv("event", EVENT), kv("outcome", "ok"), kv("topic", record.topic()), kv("key", record.key()?.toString()),
            kv("partition", metadata.partition()), kv("offset", metadata.offset()), kv("payload", payload(record.value())),
        )
    }

    fun <K, V> failed(record: ProducerRecord<K, V>, error: Throwable) {
        log.error(
            "kafka publish failed {} key={}: {}",
            record.topic(), record.key(), error.message,
            kv("event", EVENT), kv("outcome", "failed"), kv("topic", record.topic()), kv("key", record.key()?.toString()),
            kv("error", error.toString()), kv("payload", payload(record.value())),
            error,
        )
    }

    /** 보낸 값을 JSON 으로(직렬화가 안 되면 toString). 너무 길면 앞부분만 두고 잘린 길이를 붙인다. */
    fun payload(value: Any?): String {
        val json = runCatching { mapper.writeValueAsString(value) }.getOrElse { value.toString() }
        return if (json.length <= MAX_PAYLOAD) json else json.take(MAX_PAYLOAD) + "…[truncated ${json.length - MAX_PAYLOAD} chars]"
    }
}
