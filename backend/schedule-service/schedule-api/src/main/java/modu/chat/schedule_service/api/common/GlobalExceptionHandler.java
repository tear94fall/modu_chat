package modu.chat.schedule_service.api.common;

import jakarta.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import modu.chat.schedule_service.application.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 모든 오류를 { "message": …, "code": … } 로 답한다.
 * - CustomException → ErrorCode 의 상태(400/404)
 * - 없는 대상(NoSuchElement, EntityNotFound) → 404
 * - 잘못된 인자·읽을 수 없는 본문 → 400
 * - Spring MVC 가 상태를 정한 예외(없는 경로 404, 메서드 405 등) → 그 상태 그대로
 * - 나머지 → 500 (원인은 로그에만 남기고 본문에는 싣지 않는다)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_MESSAGE = "잘못된 요청입니다.";

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponse.of(e));
    }

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return respond(HttpStatus.NOT_FOUND, ErrorResponse.NOT_FOUND, "대상을 찾을 수 없습니다.");
    }

    /** @Valid 실패는 400 과 첫 번째 필드 메시지. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map((FieldError f) -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(INVALID_MESSAGE);
        return respond(HttpStatus.BAD_REQUEST, ErrorResponse.INVALID_REQUEST, message);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorResponse.INVALID_REQUEST, INVALID_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse known) {
            HttpStatusCode status = known.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String code = resolved != null ? resolved.name() : String.valueOf(status.value());
            String detail = known.getBody().getDetail();
            String message = detail != null ? detail : (e.getMessage() != null ? e.getMessage() : code);
            return respond(status, status.value() == 400 ? ErrorResponse.INVALID_REQUEST : code, message);
        }
        log.error("처리하지 못한 예외", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse.INTERNAL_ERROR, "요청을 처리하지 못했습니다.");
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, code));
    }
}
