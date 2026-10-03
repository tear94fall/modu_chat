package com.example.memberservice.api.logging

import feign.RequestTemplate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class RequestIdFeignInterceptorTest {

    private val interceptor = RequestIdFeignInterceptor()

    @AfterEach
    fun tearDown() = MDC.clear()

    @Test
    fun addsHeaderFromMdc() {
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-abc")
        val template = RequestTemplate()
        interceptor.apply(template)
        assertEquals(listOf("req-abc"), template.headers()[RequestContext.REQUEST_ID_HEADER]?.toList())
    }

    @Test
    fun addsNothingWithoutMdc() {
        val template = RequestTemplate()
        interceptor.apply(template)
        assertNull(template.headers()[RequestContext.REQUEST_ID_HEADER])
    }
}
