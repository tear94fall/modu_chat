package modu.chat.schedule_service.comoon.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import modu.chat.schedule_service.schedule.entity.Schedule;
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

@Slf4j
@Service
public class DynamicJobScheduler {

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DynamicJobScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 동시 실행 가능하도록 스레드 풀 설정
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    public void addScheduleJob(Schedule schedule) {
        if (scheduledTasks.containsKey(schedule.getId())) {
            System.out.println("Job [" + schedule.getId() + "] 이미 실행 중!");
            return;
        }

        Runnable task = (() -> {
            if (schedule.getProtocol().equals(Protocol.REST_API)) {
                String response = "";

                WebClient webClient = WebClient.builder()
                        .baseUrl(String.format("https://%s:%d", schedule.getAddress(), schedule.getPort()))
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

                if (schedule.getMethod().equals(HttpMethod.GET.toString())) {
                    response = getMethodCall(webClient, schedule);
                } else if (schedule.getMethod().equals(HttpMethod.POST.toString())) {
                    response = postMethodCall(webClient, schedule);
                }

                log.info("response: " + response);
            } else {
                log.info("protocol: {} is not supported", schedule.getProtocol());
            }

            System.out.println("[Dynamic Job] 실행됨! ID: " + schedule.getId() + ", 설명: " + schedule.getDescription() + ", 시간: " + Instant.now());
        });

        ScheduledFuture<?> scheduledTask = ((ThreadPoolTaskScheduler) taskScheduler)
                .schedule(task, new CronTrigger(schedule.getCronExpression()));

        scheduledTasks.put(schedule.getId(), scheduledTask);
        System.out.println("Job [" + schedule.getId() + "] 추가됨. Cron: " + schedule.getCronExpression());
    }

    public void addAllScheduleJobs(List<Schedule> scheduleList) {
        scheduleList.forEach(this::addScheduleJob);
    }

    public void updateScheduleJob(Schedule schedule) {
        if (!scheduledTasks.containsKey(schedule.getId())) {
            System.out.println("Job [" + schedule.getId() + "]가 존재하지 않습니다.");
            return;
        }

        // 기존 Job 제거
        removeScheduleJob(schedule.getId());

        // 새로운 Job 추가
        addScheduleJob(schedule);

        System.out.println("Job [" + schedule.getId() + "] 업데이트 완료. 새로운 Cron: " + schedule.getCronExpression());
    }

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

    public String getMethodCall(WebClient webClient, Schedule schedule) {
        return webClient.get()
                .uri(uriBuilder -> {
                    if (schedule.getDataType().equals(DataType.STRING)) {
                        uriBuilder.path(String.format("%s/%s", schedule.getPath(), schedule.getDataValue()));
                    } else if (schedule.getDataType().equals(DataType.JSON)) {
                        uriBuilder.path(schedule.getPath());

                        Map<String, Object> params = convertQueryParams(schedule.getDataValue());
                        params.forEach(uriBuilder::queryParam);
                    } else if (schedule.getDataType().equals(DataType.NONE)) {
                        uriBuilder.path(schedule.getPath());
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String postMethodCall(WebClient webClient, Schedule schedule) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(schedule.getPath()).build())
                .bodyValue(schedule.getDataValue())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
