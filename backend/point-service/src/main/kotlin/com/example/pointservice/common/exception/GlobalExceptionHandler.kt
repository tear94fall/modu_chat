package com.example.pointservice.common.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustom(e: CustomException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.errorCode.status).body(ErrorResponse.of(e))

    /** `@Valid` 실패는 400 과 첫 번째 필드 메시지. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalid(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.let { "${it.field}: ${it.defaultMessage}" } ?: "잘못된 요청입니다."
        return ResponseEntity.badRequest().body(ErrorResponse(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message))
    }
}
