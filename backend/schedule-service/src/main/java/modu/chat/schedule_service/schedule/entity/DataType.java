package modu.chat.schedule_service.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DataType {

    JSON("JSON"),
    STRING("STRING"),
    XML("XML"),
    NONE("NONE")
    ;

    private final String type;

    public static DataType fromString(String type) {
        return Arrays.stream(DataType.values())
                .filter(t -> t.getType().equals(type))
                .findAny().orElse(null);
    }
}
