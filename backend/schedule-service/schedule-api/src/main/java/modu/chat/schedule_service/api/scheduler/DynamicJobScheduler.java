package modu.chat.schedule_service.api.scheduler;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import modu.chat.schedule_service.api.logging.JobRunLog;
import modu.chat.schedule_service.api.logging.RequestContext;
import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.scheduler.JobScheduler;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 예약 작업 실행기(JobScheduler 포트의 구현). cron 주기마다 스케줄이 가리키는 다른 서비스의 HTTP 엔드포인트를 부른다.
 * DB 트랜잭션과는 무관하게, 유스케이스가 쓰기를 끝낸 뒤에 불린다.
 *
 * <p>파드마다 저장된 스케줄을 전부 거므로(ScheduleJobInitializer) 파드가 2개면 같은 cron 이 두 번 울린다. 그래서 작업
 * 본문은 {@code @Scheduled} 의 {@code @SchedulerLock} 과 같은 뜻으로 {@link LockingTaskExecutor} 로 감싼다 — 스케줄 id
 * 단위 이름({@code schedule:job-<id>})으로 master DB 의 {@code shedlock} 행을 먼저 잡은 파드만 호출하고, 나머지는 그 회차를
 * 건너뛴다(SchedulerLockConfig).
 */
@Slf4j
@Service
public class DynamicJobScheduler implements JobScheduler {

    /** 락 이름. 서비스 안에서 유일하고 {@code shedlock.name VARCHAR(64)} 안에 든다. */
    static final String LOCK_NAME_PREFIX = "schedule:job-";
    /** 호출 하나는 HTTP 한 번이라 짧다. 이 시간이 지나면 락을 쥔 파드가 죽었다고 보고 다른 파드가 가져간다. */
    static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(2);
    /** 파드 간 시계 차이·거의 동시에 울리는 cron 때문에 본문이 1초 만에 끝나도 이만큼은 락을 쥐고 있어 두 번 돌지 않는다. */
    static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(30);

    private final TaskScheduler taskScheduler;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJobScheduler(LockingTaskExecutor lockingTaskExecutor) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 동시 실행 가능하도록 스레드 풀 설정
        scheduler.initialize();
        this.taskScheduler = scheduler;
        this.lockingTaskExecutor = lockingTaskExecutor;
    }

    static String lockName(Long scheduleId) {
        return LOCK_NAME_PREFIX + scheduleId;
    }

    /**
     * 작업 본문을 스케줄 id 락 아래에서 돌린다. 다른 파드(또는 같은 파드의 직전 회차)가 락을 쥐고 있으면 본문은 돌지 않는다.
     * cron 이 울릴 때마다 불리고, 테스트가 락 동작을 보려고 직접 부르기도 한다.
     */
    void runLocked(Long scheduleId, Runnable job) {
        lockingTaskExecutor.executeWithLock(
                job,
                new LockConfiguration(Instant.now(), lockName(scheduleId), LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR));
    }

    @Override
    public void addScheduleJob(ScheduleResult schedule) {
        if (scheduledTasks.containsKey(schedule.id())) {
            log.info("schedule {} already registered", schedule.id());
            return;
        }

        Runnable job = jobFor(schedule);
        ScheduledFuture<?> scheduledTask = ((ThreadPoolTaskScheduler) taskScheduler)
                .schedule(() -> runLocked(schedule.id(), job), new CronTrigger(schedule.cronExpression()));

        scheduledTasks.put(schedule.id(), scheduledTask);
        log.info("schedule {} registered, cron {}", schedule.id(), schedule.cronExpression());
    }

    /**
     * 스케줄 한 회차의 본문. 회차마다 새 requestId(job- 접두)와 작업 이름(schedule-&lt;id&gt;)을 MDC 에 넣어 이 안의 로그와 바깥 호출(X-Request-Id)이
     * 같은 id 를 갖게 하고, 끝나면 {@code job.run} 이벤트 한 줄(JobRunLog — target·outcome·durationMs·error)을 남기고 MDC 를 지운다.
     * 실패는 여기서 삼킨다(이벤트로 남기고 다음 회차는 그대로 돈다). 테스트가 직접 부른다.
     */
    Runnable jobFor(ScheduleResult schedule) {
        return () -> {
            String requestId = "job-" + RequestContext.newId();
            String job = lockName(schedule.id());
            MDC.put(RequestContext.MDC_REQUEST_ID, requestId);
            MDC.put(RequestContext.MDC_JOB, job);
            long started = System.nanoTime();
            try {
                if (!Protocol.REST_API.equals(schedule.protocol())) {
                    JobRunLog.skipped(job, schedule.id(), "protocol " + schedule.protocol() + " is not supported");
                    return;
                }
                String target = targetOf(schedule);
                try {
                    String response = runJob(schedule, requestId);
                    JobRunLog.ok(job, schedule.id(), target, elapsedMs(started), response);
                } catch (RuntimeException e) {
                    JobRunLog.failed(job, schedule.id(), target, elapsedMs(started), e);
                }
            } finally {
                MDC.remove(RequestContext.MDC_REQUEST_ID);
                MDC.remove(RequestContext.MDC_JOB);
            }
        };
    }

    /** 로그용 호출 대상: {@code GET https://host:port/path}. */
    static String targetOf(ScheduleResult schedule) {
        return String.format("%s https://%s:%d%s", schedule.method(), schedule.address(), schedule.port(), schedule.path());
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    /** 스케줄이 가리키는 REST 엔드포인트를 부르고 응답 본문을 돌려준다. */
    private String runJob(ScheduleResult schedule, String requestId) {
        WebClient webClient = WebClient.builder()
                .baseUrl(String.format("https://%s:%d", schedule.address(), schedule.port()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(RequestContext.REQUEST_ID_HEADER, requestId)
                .build();

        if (schedule.method().equals(HttpMethod.GET.toString())) {
            return getMethodCall(webClient, schedule);
        } else if (schedule.method().equals(HttpMethod.POST.toString())) {
            return postMethodCall(webClient, schedule);
        }
        return "";
    }

    public void addAllScheduleJobs(List<ScheduleResult> scheduleList) {
        scheduleList.forEach(this::addScheduleJob);
    }

    @Override
    public void updateScheduleJob(ScheduleResult schedule) {
        if (!scheduledTasks.containsKey(schedule.id())) {
            log.info("schedule {} is not registered", schedule.id());
            return;
        }

        // 기존 Job 제거
        removeScheduleJob(schedule.id());

        // 새로운 Job 추가
        addScheduleJob(schedule);

        log.info("schedule {} updated, cron {}", schedule.id(), schedule.cronExpression());
    }

    @Override
    public void removeScheduleJob(Long jobId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(jobId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(jobId);
            log.info("schedule {} removed", jobId);
        } else {
            log.info("schedule {} is not registered", jobId);
        }
    }

    @Override
    public void removeAllScheduleJob() {
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
        log.info("all schedules removed");
    }

    public Long searchScheduleJob(Long jobId) {
        if (!scheduledTasks.containsKey(jobId)) {
            log.info("schedule {} is not registered", jobId);
            return -1L;
        }

        return jobId;
    }

    public List<Long> searchAllScheduleJob() {
        List<Long> scheduleIdList = new ArrayList<>();

        if (scheduledTasks.isEmpty()) {
            log.info("no schedules registered");
        } else {
            scheduleIdList.addAll(scheduledTasks.keySet());
        }

        return scheduleIdList;
    }

    public void listAllScheduleJobs() {
        if (scheduledTasks.isEmpty()) {
            log.info("no schedules registered");
        } else {
            log.info("registered schedules: {}", scheduledTasks.keySet());
        }
    }

    // api call
    public Map<String, Object> convertQueryParams(String data) {
        Map<String, Object> params = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            params = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException e) {
            log.error(e.getMessage());
        }

        return params;
    }

    public String getMethodCall(WebClient webClient, ScheduleResult schedule) {
        return webClient.get()
                .uri(uriBuilder -> {
                    if (schedule.dataType().equals(DataType.STRING)) {
                        uriBuilder.path(String.format("%s/%s", schedule.path(), schedule.dataValue()));
                    } else if (schedule.dataType().equals(DataType.JSON)) {
                        uriBuilder.path(schedule.path());

                        Map<String, Object> params = convertQueryParams(schedule.dataValue());
                        params.forEach(uriBuilder::queryParam);
                    } else if (schedule.dataType().equals(DataType.NONE)) {
                        uriBuilder.path(schedule.path());
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String postMethodCall(WebClient webClient, ScheduleResult schedule) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(schedule.path()).build())
                .bodyValue(schedule.dataValue())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
