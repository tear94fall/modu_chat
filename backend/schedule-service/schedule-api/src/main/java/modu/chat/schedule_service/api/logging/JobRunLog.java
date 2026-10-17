package modu.chat.schedule_service.api.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * 스케줄 작업 한 회차의 결과를 {@code job.run} 이벤트 한 줄로 남긴다 — 어느 스케줄(job=schedule-&lt;id&gt;)이 어디(target)를 불러
 * 어떻게 끝났는지(outcome ok/failed/skipped, durationMs, 실패면 error 와 스택). 커머스 배치(JobMdc)와 같은 이벤트 이름이라
 * OpenSearch 에서 {@code attributes.event:"job.run"} 하나로 두 서비스의 배치 실행 이력을 본다.
 * 부르는 쪽이 MDC 에 requestId(job- 접두)와 job 을 넣어 둔 상태에서 불러야 그 값이 함께 붙는다.
 */
public final class JobRunLog {

    public static final String EVENT = "job.run";
    public static final String LOGGER = "job.run";
    /** 응답 본문은 이만큼만 남긴다. */
    public static final int MAX_RESPONSE = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER);

    private JobRunLog() {
    }

    public static void ok(String job, Long scheduleId, String target, long durationMs, String response) {
        LOG.info("job {} ok {}ms {}", job, durationMs, target,
                kv("event", EVENT), kv("job", job), kv("scheduleId", scheduleId), kv("target", target),
                kv("outcome", "ok"), kv("durationMs", durationMs), kv("response", truncate(response)));
    }

    public static void failed(String job, Long scheduleId, String target, long durationMs, Throwable error) {
        LOG.error("job {} failed {}ms {}: {}", job, durationMs, target, error.getMessage(),
                kv("event", EVENT), kv("job", job), kv("scheduleId", scheduleId), kv("target", target),
                kv("outcome", "failed"), kv("durationMs", durationMs), kv("error", error.toString()), error);
    }

    public static void skipped(String job, Long scheduleId, String reason) {
        LOG.warn("job {} skipped: {}", job, reason,
                kv("event", EVENT), kv("job", job), kv("scheduleId", scheduleId), kv("outcome", "skipped"), kv("reason", reason));
    }

    static String truncate(String response) {
        if (response == null) {
            return null;
        }
        return response.length() <= MAX_RESPONSE ? response : response.substring(0, MAX_RESPONSE) + "…[truncated " + (response.length() - MAX_RESPONSE) + " chars]";
    }
}
