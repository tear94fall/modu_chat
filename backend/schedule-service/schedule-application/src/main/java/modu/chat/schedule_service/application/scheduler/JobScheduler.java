package modu.chat.schedule_service.application.scheduler;

import modu.chat.schedule_service.application.usecase.result.ScheduleResult;

/**
 * 예약 작업을 실제로 돌리는 쪽(포트). 구현은 schedule-api 의 DynamicJobScheduler 로,
 * cron 주기마다 스케줄이 가리키는 다른 서비스의 HTTP 엔드포인트를 부른다.
 */
public interface JobScheduler {

    void addScheduleJob(ScheduleResult schedule);

    void updateScheduleJob(ScheduleResult schedule);

    void removeScheduleJob(Long jobId);

    void removeAllScheduleJob();
}
