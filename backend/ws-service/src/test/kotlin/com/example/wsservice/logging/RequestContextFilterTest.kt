package com.example.wsservice.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RequestContextFilterTest {

    private val filter = RequestContextFilter()
    private val accessLogger = LoggerFactory.getLogger(RequestContextFilter.ACCESS_LOGGER) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    /** 체인 안에서 보인 MDC 값. 필터가 요청 동안 넣고 끝나면 지우는지 본다. */
    private var seenRequestId: String? = null
    private var seenUserId: String? = null

    @BeforeEach
    fun setUp() {
        appender.start()
        accessLogger.addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        accessLogger.detachAppender(appender)
        MDC.clear()
    }

    private fun run(uri: String = "/api-public/x", requestId: String? = null, userId: String? = null, status: Int = 200): MockHttpServletResponse {
        val request = MockHttpServletRequest("GET", uri)
        if (requestId != null) request.addHeader(RequestContext.REQUEST_ID_HEADER, requestId)
        if (userId != null) request.addHeader(RequestContext.USER_ID_HEADER, userId)
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, res ->
            seenRequestId = MDC.get(RequestContext.MDC_REQUEST_ID)
            seenUserId = MDC.get(RequestContext.MDC_USER_ID)
            (res as MockHttpServletResponse).status = status
        }
        filter.doFilter(request, response, chain)
        return response
    }

    @Test
    fun missingHeader_generatesIdAndSetsResponseHeader() {
        val response = run()
        val id = response.getHeader(RequestContext.REQUEST_ID_HEADER)
        assertNotNull(id)
        assertEquals(16, id!!.length)
        assertTrue(RequestContext.isValid(id))
        assertEquals(id, seenRequestId)
    }

    @Test
    fun validHeader_isEchoed() {
        val response = run(requestId = "abc-123_XYZ")
        assertEquals("abc-123_XYZ", response.getHeader(RequestContext.REQUEST_ID_HEADER))
        assertEquals("abc-123_XYZ", seenRequestId)
    }

    @Test
    fun invalidHeader_isReplacedWithNewId() {
        val response = run(requestId = "bad id\nwith newline")
        val id = response.getHeader(RequestContext.REQUEST_ID_HEADER)
        assertNotEquals("bad id\nwith newline", id)
        assertTrue(RequestContext.isValid(id))
    }

    @Test
    fun tooLongHeader_isReplacedWithNewId() {
        val long = "a".repeat(65)
        val id = run(requestId = long).getHeader(RequestContext.REQUEST_ID_HEADER)
        assertNotEquals(long, id)
        assertTrue(RequestContext.isValid(id))
    }

    @Test
    fun userIdHeader_isPutInMdcOnlyWhenPresent() {
        run(userId = "42")
        assertEquals("42", seenUserId)
        run()
        assertNull(seenUserId)
    }

    @Test
    fun mdc_isClearedAfterRequest() {
        run(requestId = "req-1", userId = "7")
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID))
        assertNull(MDC.get(RequestContext.MDC_USER_ID))
    }

    @Test
    fun mdc_isClearedEvenWhenChainThrows() {
        val request = MockHttpServletRequest("GET", "/api-public/x")
        assertThrows<IllegalStateException> {
            filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> throw IllegalStateException("boom") })
        }
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID))
        // 예외로 끝난 요청은 500 으로 적는다.
        assertEquals(500, appender.list.single().argumentArray[2].toString().toInt())
    }

    @Test
    fun accessLog_isWrittenWithMethodPathStatus() {
        run(uri = "/api-public/rooms", status = 201)
        val event = appender.list.single()
        assertEquals("GET /api-public/rooms 201 ${event.argumentArray[3]}ms", event.formattedMessage)
    }

    @Test
    fun accessLog_isSkippedForActuator() {
        run(uri = "/actuator/health")
        assertTrue(appender.list.isEmpty())
    }
}
