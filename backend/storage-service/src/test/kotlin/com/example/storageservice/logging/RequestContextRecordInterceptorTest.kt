package com.example.storageservice.logging

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.MDC

class RequestContextRecordInterceptorTest {

    private val interceptor = RequestContextRecordInterceptor<String, String>()

    @Suppress("UNCHECKED_CAST")
    private val consumer = Mockito.mock(Consumer::class.java) as Consumer<String, String>

    @AfterEach
    fun tearDown() = MDC.clear()

    private fun record(requestId: String? = null, userId: String? = null): ConsumerRecord<String, String> {
        val record = ConsumerRecord("topic", 0, 0L, "key", "value")
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
}
