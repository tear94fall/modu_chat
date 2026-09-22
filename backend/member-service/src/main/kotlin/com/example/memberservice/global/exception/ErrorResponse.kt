package com.example.memberservice.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ErrorResponse(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    constructor(errorCode: ErrorCode) : this(errorCode.status, errorCode.name, errorCode.message)

    companion object {
        fun error(e: CustomException): ResponseEntity<ErrorResponse> {
            val message = if (e.errorTarget.isBlank()) e.errorCode.message else "${e.errorCode.message} ID: ${e.errorTarget}"
            return ResponseEntity
                .status(e.errorCode.status)
                .body(ErrorResponse(e.errorCode.status, e.errorCode.name, message))
        }
    }
}
