package modu.chat.schedule_service.schedule.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Protocol {
    REST_API("REST_API"),
    GRPC("GRPC"),
    GRAPHQL("GRAPHQL"),
    ;

    private final String protocol;

    public static Protocol fromString(String protocol) {
        return Arrays.stream(Protocol.values())
                .filter(p -> p.getProtocol().equals(protocol))
                .findAny().orElse(null);
    }
}
