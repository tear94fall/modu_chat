package com.example.memberservice.api.admin.dto

import com.example.memberservice.application.domain.repository.query.FriendCounts
import com.example.memberservice.application.usecase.result.AdminFriendPageResult

/** 백오피스 친구 탭 응답. counts 는 필터와 상관없이 이 회원의 모든 친구 행 기준이다. */
class AdminFriendPageDto(
    val content: List<AdminFriendDto>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val counts: FriendCounts,
) {
    companion object {
        fun of(r: AdminFriendPageResult) = AdminFriendPageDto(
            r.content.map(AdminFriendDto::of), r.totalElements, r.totalPages, r.number, r.size, r.counts,
        )
    }
}
