package modu.chat.schedule_service.api.scheduler;

import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.application.usecase.ScheduleUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 기동이 끝나면 저장된 스케줄을 모두 작업 스케줄러에 건다. */
@Component
@RequiredArgsConstructor
public class ScheduleJobInitializer implements ApplicationRunner {

    private final ScheduleUseCase scheduleUseCase;

    @Override
    public void run(ApplicationArguments args) {
        scheduleUseCase.registerAllSchedules();
    }
}
