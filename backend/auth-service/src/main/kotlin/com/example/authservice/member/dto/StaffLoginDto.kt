package com.example.authservice.member.dto

/** member-service 가 돌려주는 직원 정보. permissions 는 SUPER, ADMIN, SYSTEM, INTERNAL 중 하나 이상. */
data class StaffLoginDto(
    var userId: String? = null,
    var permissions: List<String> = emptyList(),
)
