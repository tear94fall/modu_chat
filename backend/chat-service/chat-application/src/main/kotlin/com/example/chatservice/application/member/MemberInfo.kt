package com.example.chatservice.application.member

/** member-service 가 돌려주는 회원 정보. 앱 응답의 방 멤버로 그대로 나간다. */
data class MemberInfo(
    val id: Long? = null,
    val userId: String? = null,
    val auth: String? = null,
    val role: Role? = null,
    val email: String? = null,
    val username: String? = null,
    val statusMessage: String? = null,
    val profileImage: String? = null,
    val wallpaperImage: String? = null,
)
