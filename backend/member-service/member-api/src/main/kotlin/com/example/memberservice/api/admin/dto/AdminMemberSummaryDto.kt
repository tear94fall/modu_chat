package com.example.memberservice.api.admin.dto

import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import java.time.LocalDateTime

/** 백오피스 회원 목록에 노출할 요약 정보. */
class AdminMemberSummaryDto(
    val id: Long?,
    val userId: String?,
    val username: String?,
    val profileImage: String?,
    val email: String?,
    val role: Role?,
    val createdDate: LocalDateTime?,
    /** 회원 상세의 친구 목록에서만 채운다(그 회원이 정한 친구 이름). 회원 검색 결과에서는 null. */
    val friendName: String?,
    /**
     * 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL). 비어 있으면 직원이 아니다.
     * role 칼럼(ROLE_ADMIN/ROLE_MEMBER)은 콘솔 권한과 상관없는 옛 값이라, 백오피스는 이것을 보여 준다.
     */
    val staffPermissions: List<StaffPermission> = emptyList(),
    /** 이용 서비스(CHAT, COMMERCE 순). 회원 목록에서만 채운다. 비어 있으면 아직 이용 기록이 없다. */
    val services: List<ModuService> = emptyList(),
    /** 회원 상태(ACTIVE | WITHDRAWN). */
    val status: MemberStatus = MemberStatus.ACTIVE,
) {
    companion object {
        fun of(r: AdminMemberSummaryResult) = AdminMemberSummaryDto(
            r.id, r.userId, r.username, r.profileImage, r.email, r.role, r.createdDate, r.friendName,
            r.staffPermissions, r.services, r.status,
        )
    }
}
