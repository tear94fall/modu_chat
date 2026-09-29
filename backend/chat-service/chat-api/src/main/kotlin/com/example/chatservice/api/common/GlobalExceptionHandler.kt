package com.example.chatservice.api.common

import com.example.chatservice.application.common.exception.CustomException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 모든 오류를 `{ "message": …, "code": … }` 로 답한다.
 * - [CustomException] → ErrorCode 의 상태(400/403/404/503)
 * - 없는 대상(NoSuchElement, EntityNotFound) → 404
 * - 검증 실패·잘못된 인자·읽을 수 없는 본문 → 400
 * - Spring MVC 가 상태를 정한 예외(없는 경로 404, 메서드 405, 빠진 헤더·파라미터 400 등) → 그 상태 그대로
 * - 나머지 → 500 (원인은 로그에만 남기고 본문에는 싣지 않는다)
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CustomException::class)
    fun handleCustom(e: CustomException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.errorCode.status).body(ErrorResponse.of(e))

    @ExceptionHandler(NoSuchElementException::class, EntityNotFoundException::class)
    fun handleNotFound(e: RuntimeException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, ErrorResponse.NOT_FOUND, "대상을 찾을 수 없습니다.")

    /** `@Valid` 실패는 400 과 첫 번째 필드 메시지. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalid(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.let { "${it.field}: ${it.defaultMessage}" } ?: INVALID_MESSAGE
        return respond(HttpStatus.BAD_REQUEST, ErrorResponse.INVALID_REQUEST, message)
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, ErrorResponse.INVALID_REQUEST, INVALID_MESSAGE)

    @ExceptionHandler(Exception::class)
    fun handleOthers(e: Exception): ResponseEntity<ErrorResponse> {
        if (e is org.springframework.web.ErrorResponse) {
            val status = e.statusCode
            val code = HttpStatus.resolve(status.value())?.name ?: status.value().toString()
            val message = e.body.detail ?: e.message ?: code
            return respond(status, if (status.value() == 400) ErrorResponse.INVALID_REQUEST else code, message)
        }
        log.error("처리하지 못한 예외", e)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorResponse.INTERNAL_ERROR, "요청을 처리하지 못했습니다.")
    }

    private fun respond(status: HttpStatusCode, code: String, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(message, code))

    companion object {
        private const val INVALID_MESSAGE = "잘못된 요청입니다."
    }
}
