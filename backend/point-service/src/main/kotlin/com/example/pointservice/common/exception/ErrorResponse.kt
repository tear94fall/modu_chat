package com.example.pointservice.common.exception

import org.springframework.http.HttpStatus

data class ErrorResponse(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    companion object {
        fun of(e: CustomException): ErrorResponse = ErrorResponse(
            status = e.errorCode.status,
            code = e.errorCode.name,
            message = if (e.errorTarget.isBlank()) e.errorCode.message else "${e.errorCode.message} (${e.errorTarget})",
        )
    }
}
