package modu.chat.schedule_service.application.usecase.result;

import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.domain.entity.Schedule;

/** 트랜잭션 밖으로 나가는 스케줄 값. 작업 스케줄러(바깥 호출)도 엔티티가 아니라 이 값을 들고 돈다. */
public record ScheduleResult(
        Long id,
        String name,
        String address,
        String path,
        Protocol protocol,
        String method,
        int port,
        DataType dataType,
        String dataValue,
        String cronExpression,
        String description) {

    public static ScheduleResult from(Schedule schedule) {
        return new ScheduleResult(
                schedule.getId(),
                schedule.getName(),
                schedule.getAddress(),
                schedule.getPath(),
                schedule.getProtocol(),
                schedule.getMethod(),
                schedule.getPort(),
                schedule.getDataType(),
                schedule.getDataValue(),
                schedule.getCronExpression(),
                schedule.getDescription());
    }
}
