package com.example.authservice.member.dto

/** 구글 ID 토큰을 검증한 결과. member-service 내부 API 로 넘겨 회원을 찾거나 만든다. */
data class GoogleAccountDto(
    var sub: String? = null,
    var email: String? = null,
    var name: String? = null,
    var picture: String? = null,
)
