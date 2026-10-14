package com.example.chatservice.api.logging

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
}
