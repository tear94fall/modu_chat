package com.example.authservice.member.dto

/** member-service 에 서비스 이용을 알린다. userId 는 토큰 subject, clientId 는 등록 클라이언트 ID. */
data class UsageRequest(
    val userId: String,
    val clientId: String,
)
