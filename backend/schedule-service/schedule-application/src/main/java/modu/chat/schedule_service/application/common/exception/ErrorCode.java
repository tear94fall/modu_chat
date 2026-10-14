package modu.chat.schedule_service.application.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "스케줄을 찾을 수 없습니다."),
    INVALID_CRON_EXPRESSION(HttpStatus.BAD_REQUEST, "cron 식이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
