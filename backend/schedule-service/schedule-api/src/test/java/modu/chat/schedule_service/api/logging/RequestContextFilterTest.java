package modu.chat.schedule_service.api.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestContextFilterTest {

    private final RequestContextFilter filter = new RequestContextFilter();
    private final Logger accessLogger = (Logger) LoggerFactory.getLogger(RequestContextFilter.ACCESS_LOGGER);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    /** 체인 안에서 보인 MDC 값. 필터가 요청 동안 넣고 끝나면 지우는지 본다. */
    private String seenRequestId;
    private String seenUserId;

    @BeforeEach
    void setUp() {
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(appender);
        MDC.clear();
    }

    private MockHttpServletResponse run(String uri, String requestId, String userId, int status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (requestId != null) request.addHeader(RequestContext.REQUEST_ID_HEADER, requestId);
        if (userId != null) request.addHeader(RequestContext.USER_ID_HEADER, userId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            seenRequestId = MDC.get(RequestContext.MDC_REQUEST_ID);
            seenUserId = MDC.get(RequestContext.MDC_USER_ID);
            ((MockHttpServletResponse) res).setStatus(status);
        };
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void missingHeader_generatesIdAndSetsResponseHeader() throws Exception {
        MockHttpServletResponse response = run("/api-public/x", null, null, 200);
        String id = response.getHeader(RequestContext.REQUEST_ID_HEADER);
        assertNotNull(id);
        assertEquals(16, id.length());
        assertTrue(RequestContext.isValid(id));
        assertEquals(id, seenRequestId);
    }

    @Test
    void validHeader_isEchoed() throws Exception {
        MockHttpServletResponse response = run("/api-public/x", "abc-123_XYZ", null, 200);
        assertEquals("abc-123_XYZ", response.getHeader(RequestContext.REQUEST_ID_HEADER));
        assertEquals("abc-123_XYZ", seenRequestId);
    }

    @Test
    void invalidHeader_isReplacedWithNewId() throws Exception {
        String id = run("/api-public/x", "bad id\nwith newline", null, 200).getHeader(RequestContext.REQUEST_ID_HEADER);
        assertNotEquals("bad id\nwith newline", id);
        assertTrue(RequestContext.isValid(id));
    }

    @Test
    void tooLongHeader_isReplacedWithNewId() throws Exception {
        String tooLong = "a".repeat(65);
        String id = run("/api-public/x", tooLong, null, 200).getHeader(RequestContext.REQUEST_ID_HEADER);
        assertNotEquals(tooLong, id);
        assertTrue(RequestContext.isValid(id));
    }

    @Test
    void userIdHeader_isPutInMdcOnlyWhenPresent() throws Exception {
        run("/api-public/x", null, "42", 200);
        assertEquals("42", seenUserId);
        run("/api-public/x", null, null, 200);
        assertNull(seenUserId);
    }

    @Test
    void mdc_isClearedAfterRequest() throws Exception {
        run("/api-public/x", "req-1", "7", 200);
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID));
        assertNull(MDC.get(RequestContext.MDC_USER_ID));
    }

    @Test
    void mdc_isClearedEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-public/x");
        FilterChain chain = (req, res) -> { throw new IllegalStateException("boom"); };
        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, new MockHttpServletResponse(), chain));
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID));
        // 예외로 끝난 요청은 500 으로 적는다.
        assertEquals(1, appender.list.size());
        assertEquals("500", appender.list.get(0).getArgumentArray()[2].toString());
    }

    @Test
    void accessLog_isWrittenWithMethodPathStatus() throws Exception {
        run("/api-public/schedules", null, null, 201);
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals("GET /api-public/schedules 201 " + event.getArgumentArray()[3] + "ms", event.getFormattedMessage());
    }

    @Test
    void accessLog_isSkippedForActuator() throws Exception {
        run("/actuator/health", null, null, 200);
        assertTrue(appender.list.isEmpty());
    }
}
