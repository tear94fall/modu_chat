package modu.chat.schedule_service.api.scheduler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import modu.chat.schedule_service.api.logging.JobRunLog;
import modu.chat.schedule_service.api.logging.LogCapture;
import modu.chat.schedule_service.api.logging.RequestContext;
import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 동적 스케줄 한 회차는 `job.run` 이벤트 한 줄을 남긴다 — 어느 스케줄(job=schedule:job-<id>, 락 이름과 같다)이 어디를 불러 어떻게 끝났는지(ok/failed/skipped, durationMs, error).
 * OpenSearch 에서 attributes.event:"job.run" 으로 커머스 배치와 같은 방식으로 본다. 락(ShedLock)은 바깥에서 걸리므로 여기서는 본문만 돌린다.
 */
class DynamicJobRunLogTest {

    private final DynamicJobScheduler scheduler = new DynamicJobScheduler(mock(LockingTaskExecutor.class));
    private LogCapture capture;

    @BeforeEach
    void setUp() {
        capture = new LogCapture(JobRunLog.LOGGER);
    }

    @AfterEach
    void tearDown() {
        capture.close();
        MDC.clear();
    }

    private static ScheduleResult schedule(Protocol protocol, int port) {
        return new ScheduleResult(77L, "테스트", "localhost", "/api-internal/ping", protocol, "GET", port, DataType.JSON, "{}", "*/5 * * * * *", "로그 테스트");
    }

    @Test
    void unreachableEndpointIsAFailedJobRunEventWithErrorAndJobRequestId() {
        scheduler.jobFor(schedule(Protocol.REST_API, 1)).run();   // 아무도 듣지 않는 포트

        ILoggingEvent event = capture.events().get(0);
        Map<String, Object> fields = LogCapture.fields(event);
        assertEquals(1, capture.events().size());
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("job.run", fields.get("event"));
        assertEquals("schedule:job-77", fields.get("job"));
        assertEquals(77, fields.get("scheduleId"));
        assertEquals("GET https://localhost:1/api-internal/ping", fields.get("target"));
        assertEquals("failed", fields.get("outcome"));
        assertTrue(((Number) fields.get("durationMs")).longValue() >= 0);
        assertTrue(fields.get("error").toString().length() > 0, "error 필드가 있어야 한다");
        assertTrue(event.getMDCPropertyMap().get(RequestContext.MDC_REQUEST_ID).startsWith("job-"));
        assertEquals("schedule:job-77", event.getMDCPropertyMap().get(RequestContext.MDC_JOB));
        assertNull(MDC.get(RequestContext.MDC_REQUEST_ID), "회차가 끝나면 MDC 를 지운다");
    }

    @Test
    void unsupportedProtocolIsASkippedJobRunEvent() {
        scheduler.jobFor(schedule(Protocol.GRPC, 1234)).run();

        ILoggingEvent event = capture.events().get(0);
        Map<String, Object> fields = LogCapture.fields(event);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("job.run", fields.get("event"));
        assertEquals("schedule:job-77", fields.get("job"));
        assertEquals("skipped", fields.get("outcome"));
        assertTrue(fields.get("reason").toString().contains("GRPC"), fields.get("reason").toString());
    }
}
