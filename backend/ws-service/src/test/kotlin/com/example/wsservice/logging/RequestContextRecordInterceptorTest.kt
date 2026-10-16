package com.example.wsservice.logging

import ch.qos.logback.classic.Level
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.MDC

class RequestContextRecordInterceptorTest {

    private val interceptor = RequestContextRecordInterceptor<String, String>()

    @Suppress("UNCHECKED_CAST")
    private val consumer = Mockito.mock(Consumer::class.java) as Consumer<String, String>

    private lateinit var capture: LogCapture

    @BeforeEach
    fun setUp() {
        capture = LogCapture(KafkaConsumeLog.LOGGER)
    }

    @AfterEach
    fun tearDown() {
        capture.close()
        MDC.clear()
    }

    private fun record(requestId: String? = null, userId: String? = null): ConsumerRecord<String, String> {
        val record = ConsumerRecord("topic", 2, 17L, "key", "value")
        if (requestId != null) record.headers().add(RequestContext.REQUEST_ID_HEADER, requestId.toByteArray())
        if (userId != null) record.headers().add(RequestContext.USER_ID_HEADER, userId.toByteArray())
        return record
    }

    @Test
    fun headerRequestId_isPutInMdc() {
        val record = record(requestId = "req-kafka-1", userId = "42")
        interceptor.intercept(record, consumer)
        assertEquals("req-kafka-1", MDC.get(RequestContext.MDC_REQUEST_ID))
        assertEquals("42", MDC.get(RequestContext.MDC_USER_ID))
    }

    @Test
    fun missingHeader_generatesFreshId() {
        interceptor.intercept(record(), consumer)
        val id = MDC.get(RequestContext.MDC_REQUEST_ID)
        assertTrue(RequestContext.isValid(id))
        assertNull(MDC.get(RequestContext.MDC_USER_ID))
    }

    @Test
    fun invalidHeader_isReplaced() {
        interceptor.intercept(record(requestId = "bad id"), consumer)
        assertNotEquals("bad id", MDC.get(RequestContext.MDC_REQUEST_ID))
    }

    @Test
    fun afterRecord_clearsMdc() {
        val record = record(requestId = "req-kafka-2", userId = "7")
        interceptor.intercept(record, consumer)
        interceptor.afterRecord(record, consumer)
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID))
        assertNull(MDC.get(RequestContext.MDC_USER_ID))
    }

    /** 리스너가 정상으로 끝나면 kafka.consume 한 줄(INFO) — 어디서 온 레코드를 얼마 만에 처리했나. MDC 가 아직 살아 있어 requestId 가 붙는다. */
    @Test
    fun success_leavesKafkaConsumeOkEvent() {
        val record = record(requestId = "req-kafka-3", userId = "7")
        interceptor.intercept(record, consumer)
        interceptor.success(record, consumer)

        assertEquals(1, capture.events.size)
        val event = capture.events[0]
        assertEquals(Level.INFO, event.level)
        assertEquals("req-kafka-3", event.mdcPropertyMap["requestId"])
        val fields = LogCapture.fields(event)
        assertEquals(KafkaConsumeLog.EVENT, fields["event"])
        assertEquals("ok", fields["outcome"])
        assertEquals("topic", fields["topic"])
        assertEquals(2, fields["partition"])
        assertEquals(17, fields["offset"])
        assertEquals("key", fields["key"])
        assertEquals("\"value\"", fields["payload"])
        assertTrue((fields["durationMs"] as Number).toLong() >= 0)
        assertNull(fields["error"])
    }

    /** 리스너가 예외를 던지면 ERROR + 스택, outcome=failed, error. */
    @Test
    fun failure_leavesKafkaConsumeFailedEvent() {
        val record = record(requestId = "req-kafka-4")
        interceptor.intercept(record, consumer)
        interceptor.failure(record, IllegalStateException("boom"), consumer)

        assertEquals(1, capture.events.size)
        val event = capture.events[0]
        assertEquals(Level.ERROR, event.level)
        assertNotNull(event.throwableProxy)
        val fields = LogCapture.fields(event)
        assertEquals(KafkaConsumeLog.EVENT, fields["event"])
        assertEquals("failed", fields["outcome"])
        assertEquals("topic", fields["topic"])
        assertEquals("java.lang.IllegalStateException: boom", fields["error"])
        assertEquals("\"value\"", fields["payload"])
    }

    /** 긴 본문은 MAX_PAYLOAD 자까지만 두고 잘린 길이를 붙인다. */
    @Test
    fun payload_isTruncated() {
        val long = "x".repeat(KafkaConsumeLog.MAX_PAYLOAD + 100)
        val payload = KafkaConsumeLog.payload(long)
        assertTrue(payload.endsWith("…[truncated 102 chars]"), payload)
        assertEquals(KafkaConsumeLog.MAX_PAYLOAD, payload.substringBefore('…').length)
    }
}
