package com.example.profileservice.application.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val message: String) {
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필 기록을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "본인만 접근할 수 있습니다."),
    MEMBER_LOOKUP_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "회원 정보를 확인하지 못했습니다. 잠시 뒤 다시 시도해 주세요."),
}
