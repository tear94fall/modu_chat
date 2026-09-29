package modu.chat.schedule_service.application.usecase;

import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.scheduler.JobScheduler;
import modu.chat.schedule_service.application.usecase.command.CreateScheduleCommand;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@Transactional
@SpringBootTest
class ScheduleUseCaseTest {

    @Autowired
    private ScheduleUseCase scheduleUseCase;

    @MockitoBean
    private JobScheduler jobScheduler;

    @Test
    @DisplayName("스케줄링 생성 테스트")
    public void createScheduleTest2() {

        //given
        CreateScheduleCommand command = new CreateScheduleCommand(
                "테스트 스케줄",
                "localhost",
                "/api/v1/schedule",
                Protocol.REST_API,
                HttpMethod.GET.toString(),
                1234,
                DataType.JSON,
                "{\"data\": \"1234\"}",
                "* */1 * * * *",
                "테스트 작업 설명");

        //when
        ScheduleResult schedule = scheduleUseCase.createSchedule(command);

        //then
        assertDoesNotThrow(() -> scheduleUseCase.searchSchedule(schedule.id()));
        verify(jobScheduler).addScheduleJob(schedule);
    }
}
