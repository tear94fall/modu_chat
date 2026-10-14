package com.example.memberservice.application.usecase.result

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.Staff
import com.example.memberservice.application.domain.entity.StaffPermission
import java.time.LocalDateTime

/** auth-service 가 로그인·토큰 갱신 때 받는 직원 정보. */
data class StaffLoginResult(val userId: String, val permissions: List<StaffPermission>)

/** 모두 인터널 회원 목록 한 줄. permissions 가 비어 있으면 직원이 아니다. */
data class StaffMemberSummaryResult(
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
        fun of(member: Member, staff: Staff?) = StaffMemberSummaryResult(
            member.id!!, member.userId, member.username, member.profileImage, member.email,
            member.createdDate, member.status, staff?.sortedPermissions().orEmpty(),
        )
    }
}

/** 직원 정보. modifiedByName 은 바꾼 사람의 지금 이름이다(없으면 null). */
data class StaffInfoResult(
    val permissions: List<StaffPermission>,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val modifiedBy: String?,
    val modifiedByName: String?,
)

data class StaffMemberDetailResult(
    val id: Long,
    val userId: String,
    val username: String?,
    val email: String,
    val profileImage: String?,
    val statusMessage: String?,
    val createdDate: LocalDateTime?,
    val status: MemberStatus,
    val friendCount: Int,
    val staff: StaffInfoResult?,
)

/** 직원 목록 한 줄(최상위 관리자 전용 화면). */
data class StaffEntryResult(
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
