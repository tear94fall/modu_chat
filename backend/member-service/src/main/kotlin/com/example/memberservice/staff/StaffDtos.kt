package com.example.memberservice.staff

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.MemberStatus
import java.time.LocalDateTime

/** auth-service 가 로그인·토큰 갱신 때 받는 직원 정보. */
data class StaffLoginDto(val userId: String, val permissions: List<StaffPermission>)

/** 모두 인터널 회원 목록 한 줄. permissions 가 비어 있으면 직원이 아니다. */
data class StaffMemberSummaryDto(
    val id: Long,
    val userId: String,
    val username: String?,
    val profileImage: String?,
    val email: String,
    val createdDate: LocalDateTime?,
    val status: MemberStatus,
    val permissions: List<StaffPermission>,
) {
    companion object {
        fun of(member: Member, staff: Staff?) = StaffMemberSummaryDto(
            member.id!!, member.userId, member.username, member.profileImage, member.email,
            member.createdDate, member.status, staff?.sortedPermissions().orEmpty(),
        )
    }
}

/** 직원 정보. modifiedByName 은 바꾼 사람의 지금 이름이다(없으면 null). */
data class StaffInfoDto(
    val permissions: List<StaffPermission>,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val modifiedBy: String?,
    val modifiedByName: String?,
)

data class StaffMemberDetailDto(
    val id: Long,
    val userId: String,
    val username: String?,
    val email: String,
    val profileImage: String?,
    val statusMessage: String?,
    val createdDate: LocalDateTime?,
    val status: MemberStatus,
    val friendCount: Int,
    val staff: StaffInfoDto?,
)

/** 직원 목록 한 줄(최상위 관리자 전용 화면). */
data class StaffEntryDto(
    val memberId: Long,
    val userId: String,
    val username: String?,
    val email: String,
    val profileImage: String?,
    val permissions: List<StaffPermission>,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val modifiedBy: String?,
    val modifiedByName: String?,
)

data class StaffPermissionRequest(val permissions: List<StaffPermission>? = null)

/** 권한은 늘 같은 순서(SUPER, ADMIN, SYSTEM, INTERNAL)로 내보낸다. */
fun Staff.sortedPermissions(): List<StaffPermission> = permissions.sortedBy { it.ordinal }
