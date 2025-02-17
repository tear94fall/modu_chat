package modu.chat.schedule_service.schedule.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import modu.chat.schedule_service.comoon.entity.BaseEntity;
import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule")
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    private String path;

    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    private String method;

    private int port;

    private DataType dataType;

    private String dataValue;

    private String cronExpression;

    private String description;

    public void updateCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    @Builder
    public Schedule(Long id, String name, String address, String path, Protocol protocol, String method, int port, DataType dataType, String dataValue, String cronExpression, String description) {
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

    public static Schedule of(String name, String address, String path, Protocol protocol, String method, int port, DataType dataType, String dataValue, String cronExpression, String description) {
        return Schedule.builder()
                .name(name)
                .address(address)
                .path(path)
                .protocol(protocol)
                .method(method)
                .port(port)
                .dataType(dataType)
                .dataValue(dataValue)
                .cronExpression(cronExpression)
                .description(description)
                .build();
    }

    public static Schedule of(ScheduleDto scheduleDto) {
        return Schedule.builder()
                .id(scheduleDto.getId())
                .name(scheduleDto.getName())
                .address(scheduleDto.getAddress())
                .path(scheduleDto.getPath())
                .protocol(scheduleDto.getProtocol())
                .method(scheduleDto.getMethod())
                .port(scheduleDto.getPort())
                .dataType(scheduleDto.getDataType())
                .dataValue(scheduleDto.getDataValue())
                .cronExpression(scheduleDto.getCronExpression())
                .description(scheduleDto.getDescription())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Schedule schedule = (Schedule) o;
        return id != null && Objects.equals(id, schedule.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.intValue() : 0;
    }
}
