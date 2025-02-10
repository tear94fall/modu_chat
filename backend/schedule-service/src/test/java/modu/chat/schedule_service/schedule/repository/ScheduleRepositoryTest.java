package modu.chat.schedule_service.schedule.repository;

import jakarta.persistence.EntityNotFoundException;
import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import modu.chat.schedule_service.schedule.entity.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduleRepositoryTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    //init data
    private static Long scheduleId;
    private static final String SCHEDULE_NAME = "테스트 스케줄";
    private static final String SCHEDULE_ADDRESS = "localhost";
    private static final String SCHEDULE_PATH = "/api/v1/schedule";
    private static final Protocol SCHEDULE_PROTOCOL = Protocol.REST_API;
    private static final String SCHEDULE_METHOD = "POST";
    private static final int SCHEDULE_PORT = 1234;
    private static final DataType SCHEDULE_DATA_TYPE = DataType.JSON;
    private static final String SCHEDULE_DATA = "{\"data\": \"1234\"}";
    private static final String SCHEDULE_CRON = "* 0 0 0 0 0";
    private static final String SCHEDULE_DESC = "테스트 작업 설명";

    //test data
    private static final String SCHEDULE_CRON_UPDATE = "*/1 * * * * *";
    private static final String SCHEDULE_DESC_UPDATE = "테스트 작업 설명 업데이트됨";

    @BeforeEach
    public void init() {
        Schedule schedule = Schedule.of(
                SCHEDULE_NAME,
                SCHEDULE_ADDRESS,
                SCHEDULE_PATH,
                SCHEDULE_PROTOCOL,
                SCHEDULE_METHOD,
                SCHEDULE_PORT,
                SCHEDULE_DATA_TYPE,
                SCHEDULE_DATA,
                SCHEDULE_CRON,
                SCHEDULE_DESC
        );

        Schedule saveSchedule = scheduleRepository.save(schedule);
        scheduleId = saveSchedule.getId();
    }

    @Test
    @DisplayName("스케줄링 생성 이후 조회 테스트")
    public void createScheduleTest() {

        //given

        //when

        //then
        assertDoesNotThrow(() -> scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found")));
    }

    @Test
    @DisplayName("스케줄링 주기 업데이트 조회 테스트")
    public void updateCronExpressionTest() {

        //given

        //when
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        schedule.updateCronExpression(SCHEDULE_CRON_UPDATE);

        Schedule updateSchedule = scheduleRepository.save(schedule);

        //then
        assertEquals(schedule.getCronExpression(), updateSchedule.getCronExpression());
    }

    @Test
    @DisplayName("스케줄링 설명 업데이트 테스트")
    public void updateDescriptionTest() {

        //given

        //when
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        schedule.updateDescription(SCHEDULE_DESC_UPDATE);

        Schedule updateSchedule = scheduleRepository.save(schedule);

        //then
        assertEquals(schedule.getDescription(), updateSchedule.getDescription());
    }

    @Test
    @DisplayName("스케줄링 삭제 테스트")
    public void deleteScheduleTest() {

        //given

        //when
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        scheduleRepository.deleteById(schedule.getId());

        //then
        assertThrows(EntityNotFoundException.class, () -> scheduleRepository.findById(schedule.getId())
                .orElseThrow(EntityNotFoundException::new));
    }
}