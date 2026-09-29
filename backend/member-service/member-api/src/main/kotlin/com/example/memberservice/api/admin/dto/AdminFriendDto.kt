package com.example.memberservice.api.admin.dto

import com.example.memberservice.application.domain.entity.FriendStatus
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.usecase.result.AdminFriendResult
import java.time.LocalDateTime

/**
 * 백오피스 회원 상세 친구 탭의 한 줄. 친구 회원의 요약([AdminMemberSummaryDto] 와 같은 필드)에
 * 이 회원이 그 친구에게 매긴 별칭·즐겨찾기·상태를 더한다.
 */
class AdminFriendDto(
    val id: Long?,
    val userId: String?,
    val username: String?,
    val profileImage: String?,
    val email: String?,
    val role: Role?,
    val createdDate: LocalDateTime?,
    /** 이 회원이 정한 친구 이름. 비어 있으면 화면은 username 을 쓴다. */
    val friendName: String?,
    val staffPermissions: List<StaffPermission> = emptyList(),
    val services: List<ModuService> = emptyList(),
    /** 친구 회원의 상태(ACTIVE | WITHDRAWN). */
    val status: MemberStatus = MemberStatus.ACTIVE,
    val favorite: Boolean = false,
    /** 이 회원이 그 친구에게 매긴 상태(NORMAL | HIDDEN | BLOCKED). */
    val friendStatus: FriendStatus = FriendStatus.NORMAL,
) {
    companion object {
        fun of(r: AdminFriendResult) = AdminFriendDto(
            r.id, r.userId, r.username, r.profileImage, r.email, r.role, r.createdDate, r.friendName,
            r.staffPermissions, r.services, r.status, r.favorite, r.friendStatus,
        )
    }
}
