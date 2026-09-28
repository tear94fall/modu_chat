package com.example.memberservice.api.admin.dto

import com.example.memberservice.member.repository.FriendCounts

/** 백오피스 친구 탭 응답. counts 는 필터와 상관없이 이 회원의 모든 친구 행 기준이다. */
class AdminFriendPageDto(
    val content: List<AdminFriendDto>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val counts: FriendCounts,
)
