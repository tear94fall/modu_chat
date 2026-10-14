package com.example.pushservice.api.common

import com.example.pushservice.application.common.exception.CustomException

/** 오류 응답 본문. 상태는 HTTP 상태 코드로만 알린다. [code] 는 호출 쪽이 분기할 때 쓰는 식별자다. */
data class ErrorResponse(
    val message: String,
    val code: String,
) {
    companion object {
        const val INVALID_REQUEST = "INVALID_REQUEST"
        const val NOT_FOUND = "NOT_FOUND"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"

        fun of(e: CustomException): ErrorResponse = ErrorResponse(
            message = if (e.errorTarget.isBlank()) e.errorCode.message else "${e.errorCode.message} (${e.errorTarget})",
            code = e.errorCode.name,
        )
    }
}
