package modu.chat.schedule_service.application.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorTarget;

    public CustomException(ErrorCode errorCode) {
        this(errorCode, "");
    }

    public CustomException(ErrorCode errorCode, String errorTarget) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorTarget = errorTarget == null ? "" : errorTarget;
    }
}
