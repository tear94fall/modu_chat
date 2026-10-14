package com.example.pushservice.application.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val message: String) {
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 FCM 토큰이 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "본인만 접근할 수 있습니다."),
}
