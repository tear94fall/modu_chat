package com.example.storageservice.logging

import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class KafkaRequestHeadersTest {

    @AfterEach
    fun tearDown() = MDC.clear()

    @Test
    fun stamp_addsRequestIdAndUserIdFromMdc() {
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-p-1")
        MDC.put(RequestContext.MDC_USER_ID, "42")
        val record = KafkaRequestHeaders.stamp(ProducerRecord("topic", "key", "value"))
        assertEquals("req-p-1", String(record.headers().lastHeader(RequestContext.REQUEST_ID_HEADER).value()))
        assertEquals("42", String(record.headers().lastHeader(RequestContext.USER_ID_HEADER).value()))
    }

    @Test
    fun stamp_addsNothingWithoutMdc() {
        val record = KafkaRequestHeaders.stamp(ProducerRecord("topic", "key", "value"))
        assertNull(record.headers().lastHeader(RequestContext.REQUEST_ID_HEADER))
        assertNull(record.headers().lastHeader(RequestContext.USER_ID_HEADER))
    }

    /** 발행 결과 콜백은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있다 — 보낼 때 잡아 둔 값을 그 안에서 되살려야 로그에 requestId 가 붙는다. */
    @Test
    fun capture_runsBlockWithCapturedMdcOnAnotherThread_andClearsAfter() {
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-cb-1")
        MDC.put(RequestContext.MDC_USER_ID, "7")
        val captured = KafkaRequestHeaders.capture()
        MDC.clear()

        var seenRequestId: String? = null
        var seenUserId: String? = null
        var afterRequestId: String? = "not-checked"
        val worker = Thread {
            captured.run {
                seenRequestId = MDC.get(RequestContext.MDC_REQUEST_ID)
                seenUserId = MDC.get(RequestContext.MDC_USER_ID)
            }
            afterRequestId = MDC.get(RequestContext.MDC_REQUEST_ID)
        }
        worker.start()
        worker.join(5_000)

        assertEquals("req-cb-1", seenRequestId)
        assertEquals("7", seenUserId)
        assertNull(afterRequestId)
    }

    @Test
    fun capture_withoutMdc_runsBlockWithoutContext() {
        val captured = KafkaRequestHeaders.capture()
        var seen: String? = "not-checked"
        captured.run { seen = MDC.get(RequestContext.MDC_REQUEST_ID) }
        assertNull(seen)
    }
}
