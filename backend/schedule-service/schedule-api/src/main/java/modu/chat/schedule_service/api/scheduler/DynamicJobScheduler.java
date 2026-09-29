package modu.chat.schedule_service.api.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.scheduler.JobScheduler;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
 */
@Slf4j
@Service
public class DynamicJobScheduler implements JobScheduler {

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJobScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 동시 실행 가능하도록 스레드 풀 설정
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @Override
    public void addScheduleJob(ScheduleResult schedule) {
        if (scheduledTasks.containsKey(schedule.id())) {
            System.out.println("Job [" + schedule.id() + "] 이미 실행 중!");
            return;
        }

        Runnable task = (() -> {
            if (schedule.protocol().equals(Protocol.REST_API)) {
                String response = "";

                WebClient webClient = WebClient.builder()
                        .baseUrl(String.format("https://%s:%d", schedule.address(), schedule.port()))
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

                if (schedule.method().equals(HttpMethod.GET.toString())) {
                    response = getMethodCall(webClient, schedule);
                } else if (schedule.method().equals(HttpMethod.POST.toString())) {
                    response = postMethodCall(webClient, schedule);
                }

                log.info("response: " + response);
            } else {
                log.info("protocol: {} is not supported", schedule.protocol());
            }

            System.out.println("[Dynamic Job] 실행됨! ID: " + schedule.id() + ", 설명: " + schedule.description() + ", 시간: " + Instant.now());
        });

        ScheduledFuture<?> scheduledTask = ((ThreadPoolTaskScheduler) taskScheduler)
                .schedule(task, new CronTrigger(schedule.cronExpression()));

        scheduledTasks.put(schedule.id(), scheduledTask);
        System.out.println("Job [" + schedule.id() + "] 추가됨. Cron: " + schedule.cronExpression());
    }

    public void addAllScheduleJobs(List<ScheduleResult> scheduleList) {
        scheduleList.forEach(this::addScheduleJob);
    }

    @Override
    public void updateScheduleJob(ScheduleResult schedule) {
        if (!scheduledTasks.containsKey(schedule.id())) {
            System.out.println("Job [" + schedule.id() + "]가 존재하지 않습니다.");
            return;
        }

        // 기존 Job 제거
        removeScheduleJob(schedule.id());

        // 새로운 Job 추가
        addScheduleJob(schedule);

        System.out.println("Job [" + schedule.id() + "] 업데이트 완료. 새로운 Cron: " + schedule.cronExpression());
    }

    @Override
    public void removeScheduleJob(Long jobId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(jobId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(jobId);
            System.out.println("Job [" + jobId + "] 제거됨.");
        } else {
            System.out.println("Job [" + jobId + "] 없음.");
        }
    }

    @Override
    public void removeAllScheduleJob() {
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
        System.out.println("모든 Job 제거됨.");
    }

    public Long searchScheduleJob(Long jobId) {
        if (!scheduledTasks.containsKey(jobId)) {
            System.out.println("Job [" + jobId + "]가 존재하지 않습니다.");
            return -1L;
        }

        return jobId;
    }

    public List<Long> searchAllScheduleJob() {
        List<Long> scheduleIdList = new ArrayList<>();

        if (scheduledTasks.isEmpty()) {
            System.out.println("현재 실행 중인 Job이 없습니다.");
        } else {
            scheduleIdList.addAll(scheduledTasks.keySet());
        }

        return scheduleIdList;
    }

    public void listAllScheduleJobs() {
        if (scheduledTasks.isEmpty()) {
            System.out.println("현재 실행 중인 Job이 없습니다.");
        } else {
            System.out.println("현재 실행 중인 Job 목록:");
            scheduledTasks.keySet().forEach(jobId -> System.out.println("- " + jobId));
        }
    }

    // api call
    public Map<String, Object> convertQueryParams(String data) {
        Map<String, Object> params = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            params = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
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
