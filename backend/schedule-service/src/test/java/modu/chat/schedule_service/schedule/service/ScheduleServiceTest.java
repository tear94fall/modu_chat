package modu.chat.schedule_service.schedule.service;

import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Test
    @DisplayName("스케줄링 생성 테스트")
    public void createScheduleTest2() {

        //given
        ScheduleDto scheduleDto = ScheduleDto.builder()
                .name("테스트 스케줄")
                .address("localhost")
                .path("/api/v1/schedule")
                .protocol(Protocol.REST_API)
                .method(HttpMethod.GET.toString())
                .port(1234)
                .dataType(DataType.JSON)
                .dataValue("{\"data\": \"1234\"}")
                .cronExpression("* */1 * * * *")
                .description("테스트 작업 설명")
                .build();

        //when
        ScheduleDto schedule = scheduleService.createSchedule(scheduleDto);

        //then
        assertDoesNotThrow(() -> scheduleService.searchSchedule(schedule.getId()));
    }
}