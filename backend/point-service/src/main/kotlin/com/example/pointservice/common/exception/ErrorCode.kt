package com.example.pointservice.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val message: String) {
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "포인트 금액이 올바르지 않습니다."),
    RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "적립 규칙을 찾을 수 없습니다."),
    RULE_ALREADY_EXISTS(HttpStatus.CONFLICT, "같은 코드의 적립 규칙이 이미 있습니다."),
    RULE_IN_USE(HttpStatus.CONFLICT, "출석 체크가 쓰는 규칙이라 지울 수 없습니다. 대신 비활성으로 바꾸세요."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 계정을 찾을 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "포인트가 부족합니다."),
}
