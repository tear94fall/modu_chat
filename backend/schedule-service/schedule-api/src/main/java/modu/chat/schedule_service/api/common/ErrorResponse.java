package modu.chat.schedule_service.api.common;

import modu.chat.schedule_service.application.common.exception.CustomException;

/** 오류 응답 본문. 상태는 HTTP 상태 코드로만 알린다. code 는 호출 쪽이 분기할 때 쓰는 식별자다. */
public record ErrorResponse(String message, String code) {

    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static ErrorResponse of(CustomException e) {
        String message = e.getErrorTarget().isBlank()
                ? e.getErrorCode().getMessage()
                : e.getErrorCode().getMessage() + " (" + e.getErrorTarget() + ")";
        return new ErrorResponse(message, e.getErrorCode().name());
    }
}
