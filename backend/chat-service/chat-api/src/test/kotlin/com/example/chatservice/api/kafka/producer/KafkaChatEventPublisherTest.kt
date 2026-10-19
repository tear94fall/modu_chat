package com.example.chatservice.api.kafka.producer

import com.example.chatservice.api.logging.KafkaPublishLog
import com.example.chatservice.api.logging.LogCapture
import com.example.chatservice.api.logging.RequestContext
import com.example.chatservice.application.message.ChatMessage
import com.example.chatservice.application.message.SubscribeType
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 발행 결과는 `kafka.publish` 이벤트 한 줄로 남긴다 — 어느 토픽에 어떤 키·본문(JSON)을 보냈고 성공했는지(파티션·오프셋)/실패했는지(error).
 * 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있으므로 보낼 때의 requestId·userId 를 되살려 붙인다.
 */
class KafkaChatEventPublisherTest {

    private val kafkaTemplate: KafkaTemplate<String, ChatMessage> = mock()
    private val publisher = KafkaChatEventPublisher(kafkaTemplate, "chat.message-broadcast.v1", "chat.room-created.v1")
    private lateinit var capture: LogCapture

    @BeforeEach
    fun setUp() {
        capture = LogCapture(KafkaPublishLog.LOGGER)
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-cb-9")
        MDC.put(RequestContext.MDC_USER_ID, "11")
    }

    @AfterEach
    fun tearDown() {
        capture.close()
        MDC.clear()
    }

    /** send() 가 돌려줄 미래값. 콜백은 요청 스레드 밖(다른 스레드, MDC 없음)에서 완료시킨다 — 실제 Kafka 네트워크 스레드와 같은 조건. */
    private fun pendingSend(): CompletableFuture<SendResult<String, ChatMessage>> {
        val future = CompletableFuture<SendResult<String, ChatMessage>>()
        whenever(kafkaTemplate.send(any<ProducerRecord<String, ChatMessage>>())).thenReturn(future)
        return future
    }

    private fun onAnotherThreadWithoutMdc(block: () -> Unit) {
        Thread { MDC.clear(); block() }.also { it.start(); it.join(5_000) }
    }

    private fun awaitLog(): ILoggingEvent {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (capture.events.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        return capture.events.single()
    }

    @Test
    fun successIsOneKafkaPublishEventWithTopicKeyPayloadOffsetAndRequestContext() {
        val future = pendingSend()
        publisher.roomCreated("room-1")
        val record = ProducerRecord<String, ChatMessage>("chat.room-created.v1", "room-1", ChatMessage(SubscribeType.ROOM_CREATED, "room-1", null))
        onAnotherThreadWithoutMdc { future.complete(SendResult(record, RecordMetadata(TopicPartition("chat.room-created.v1", 2), 17, 0, 0, 0, 0))) }

        val event = awaitLog()
        val fields = LogCapture.fields(event)
        assertEquals(Level.INFO, event.level)
        assertEquals("kafka.publish", fields["event"])
        assertEquals("ok", fields["outcome"])
        assertEquals("chat.room-created.v1", fields["topic"])
        assertEquals("room-1", fields["key"])
        assertEquals(2, fields["partition"])
        assertEquals(17, fields["offset"])
        assertTrue((fields["payload"] as String).contains(""""roomId":"room-1""""), fields["payload"].toString())
        assertEquals("req-cb-9", event.mdcPropertyMap[RequestContext.MDC_REQUEST_ID])
        assertEquals("11", event.mdcPropertyMap[RequestContext.MDC_USER_ID])
    }

    @Test
    fun failureIsAnErrorKafkaPublishEventWithTheErrorAndPayload() {
        val future = pendingSend()
        publisher.roomCreated("room-1")
        onAnotherThreadWithoutMdc { future.completeExceptionally(RuntimeException("kafka down")) }

        val event = awaitLog()
        val fields = LogCapture.fields(event)
        assertEquals(Level.ERROR, event.level)
        assertEquals("kafka.publish", fields["event"])
        assertEquals("failed", fields["outcome"])
        assertEquals("chat.room-created.v1", fields["topic"])
        assertEquals("room-1", fields["key"])
        assertTrue((fields["error"] as String).contains("kafka down"), fields["error"].toString())
        assertTrue((fields["payload"] as String).contains(""""roomId":"room-1""""), fields["payload"].toString())
        assertEquals("req-cb-9", event.mdcPropertyMap[RequestContext.MDC_REQUEST_ID])
    }
}
