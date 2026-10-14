package modu.chat.schedule_service.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.application.common.exception.CustomException;
import modu.chat.schedule_service.application.common.exception.ErrorCode;
import modu.chat.schedule_service.application.config.RwJpaConfig;
import modu.chat.schedule_service.application.domain.entity.Schedule;
import modu.chat.schedule_service.application.domain.repository.rw.ScheduleRwRepository;
import modu.chat.schedule_service.application.usecase.command.CreateScheduleCommand;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스케줄 쓰기(master). 작업 상태(등록·주기 변경·삭제)는 모두 여기서 쓴다.
 * 등록 직후 단건 조회와, 돌릴 작업을 정하는 기동 시 전체 조회도 최신이어야 하므로 master 에서 읽는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
public class ScheduleCommandService {

    private final ScheduleRwRepository scheduleRwRepository;

    /** 같은 name·cronExpression 이 이미 있으면 새로 만들지 않고 그것을 돌려준다(멱등). */
    public ScheduleResult createSchedule(CreateScheduleCommand command) {
        Schedule schedule = scheduleRwRepository.findByNameAndCronExpression(command.name(), command.cronExpression())
                .orElseGet(() -> scheduleRwRepository.save(Schedule.of(
                        command.name(),
                        command.address(),
                        command.path(),
                        command.protocol(),
                        command.method(),
                        command.port(),
                        command.dataType(),
                        command.dataValue(),
                        command.cronExpression(),
                        command.description())));

        return ScheduleResult.from(schedule);
    }

    public ScheduleResult updateCronExpression(Long id, String cronExpression) {
        Schedule schedule = find(id);
        schedule.updateCronExpression(cronExpression);

        return ScheduleResult.from(schedule);
    }

    public void deleteSchedule(Long id) {
        scheduleRwRepository.delete(find(id));
    }

    public void deleteAllSchedules() {
        scheduleRwRepository.deleteAll();
    }

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    public ScheduleResult searchSchedule(Long id) {
        return ScheduleResult.from(find(id));
    }

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    public List<ScheduleResult> searchAllSchedulesOnMaster() {
        return scheduleRwRepository.findAll().stream().map(ScheduleResult::from).toList();
    }

    private Schedule find(Long id) {
        return scheduleRwRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND, String.valueOf(id)));
    }
}
