package modu.chat.schedule_service.application.usecase.command;

import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;

public record CreateScheduleCommand(
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
}
