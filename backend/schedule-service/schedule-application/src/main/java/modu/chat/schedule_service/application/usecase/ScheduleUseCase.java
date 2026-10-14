package modu.chat.schedule_service.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modu.chat.schedule_service.application.common.exception.CustomException;
import modu.chat.schedule_service.application.common.exception.ErrorCode;
import modu.chat.schedule_service.application.scheduler.JobScheduler;
import modu.chat.schedule_service.application.service.ScheduleCommandService;
import modu.chat.schedule_service.application.service.ScheduleQueryService;
import modu.chat.schedule_service.application.usecase.command.CreateScheduleCommand;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * 스케줄 관리. DB 에 먼저 쓰고(트랜잭션이 끝난 뒤) 작업 스케줄러에 반영한다.
 * 작업을 거는 일이 트랜잭션 밖이라, 걸 수 없는 cron 식은 쓰기 전에 걸러 낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleUseCase {

    private final ScheduleQueryService scheduleQueryService;
    private final ScheduleCommandService scheduleCommandService;
    private final JobScheduler jobScheduler;

    /** 기동할 때 저장된 스케줄을 모두 건다. 하나가 잘못돼도 나머지는 건다. */
    public void registerAllSchedules() {
        for (ScheduleResult schedule : scheduleCommandService.searchAllSchedulesOnMaster()) {
            try {
                jobScheduler.addScheduleJob(schedule);
            } catch (RuntimeException e) {
                log.error("스케줄 [{}] 을 걸지 못했다. cron: {}", schedule.id(), schedule.cronExpression(), e);
            }
        }
    }

    public ScheduleResult createSchedule(CreateScheduleCommand command) {
        requireValidCron(command.cronExpression());
        ScheduleResult schedule = scheduleCommandService.createSchedule(command);
        jobScheduler.addScheduleJob(schedule);

        return schedule;
    }

    public List<ScheduleResult> searchAllSchedules() {
        return scheduleQueryService.searchAllSchedules();
    }

    public ScheduleResult searchSchedule(Long id) {
        return scheduleCommandService.searchSchedule(id);
    }

    public ScheduleResult updateSchedule(Long id, String cronExpression) {
        requireValidCron(cronExpression);
        ScheduleResult schedule = scheduleCommandService.updateCronExpression(id, cronExpression);
        jobScheduler.updateScheduleJob(schedule);

        return schedule;
    }

    public void deleteSchedule(Long id) {
        scheduleCommandService.deleteSchedule(id);
        jobScheduler.removeScheduleJob(id);
    }

    public void deleteAllSchedules() {
        scheduleCommandService.deleteAllSchedules();
        jobScheduler.removeAllScheduleJob();
    }

    private void requireValidCron(String cronExpression) {
        if (cronExpression == null || !CronExpression.isValidExpression(cronExpression)) {
            throw new CustomException(ErrorCode.INVALID_CRON_EXPRESSION, String.valueOf(cronExpression));
        }
    }
}
