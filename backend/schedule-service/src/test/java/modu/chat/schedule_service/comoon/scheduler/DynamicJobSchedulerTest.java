package modu.chat.schedule_service.comoon.scheduler;

import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import modu.chat.schedule_service.schedule.entity.Schedule;
import modu.chat.schedule_service.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DynamicJobSchedulerTest {

    @Autowired
    private DynamicJobScheduler scheduler;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("스케줄러에 job 추가 테스트")
    public void addScheduleJobTest() {

        //given
        Schedule schedule = Schedule.of("테스트 스케줄", "localhost", "/api/v1/schedule", Protocol.REST_API, "POST", 1234, DataType.JSON,"{\"data\": \"1234\"}", "*/1 * * * * *", "테스트 작업 설명");

        //when
        Schedule saveSchedule = scheduleRepository.save(schedule);
        scheduler.addScheduleJob(saveSchedule);

        //then
        Long jobId = scheduler.searchScheduleJob(saveSchedule.getId());

        assertEquals(saveSchedule.getId(), jobId);
    }

    @Test
    @DisplayName("스케줄러 job 삭제 테스트")
    public void removeScheduleJobTest() {

        //given

        //when

        //then
    }
}