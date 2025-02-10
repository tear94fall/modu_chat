package modu.chat.schedule_service.schedule.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import modu.chat.schedule_service.schedule.entity.Schedule;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleDto {

    private Long id;
    private String name;
    private String address;
    private String path;
    private Protocol protocol;
    private String method;
    private int port;
    private DataType dataType;
    private String dataValue;
    private String cronExpression;
    private String description;

    @Builder
    public ScheduleDto(Long id, String name, String address, String path, Protocol protocol, String method, int port, DataType dataType, String dataValue, String cronExpression, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.path = path;
        this.protocol = protocol;
        this.method = method;
        this.port = port;
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.cronExpression = cronExpression;
        this.description = description;
    }

    public static ScheduleDto from(Schedule schedule) {
        return ScheduleDto
                .builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .address(schedule.getAddress())
                .path(schedule.getPath())
                .protocol(schedule.getProtocol())
                .method(schedule.getMethod())
                .port(schedule.getPort())
                .dataType(schedule.getDataType())
                .dataValue(schedule.getDataValue())
                .cronExpression(schedule.getCronExpression())
                .description(schedule.getDescription())
                .build();
    }
}
