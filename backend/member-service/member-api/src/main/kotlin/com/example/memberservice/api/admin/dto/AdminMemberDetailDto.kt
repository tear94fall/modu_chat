package com.example.memberservice.api.admin.dto

import com.example.memberservice.api.dto.MemberDto
import com.example.memberservice.api.dto.ServiceUsageDto
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import java.time.LocalDateTime

class AdminMemberDetailDto(
    val member: MemberDto,
    val friendCount: Int,
    val createdDate: LocalDateTime?,
    /** 친구 목록. 회원 조회 화면에서 누구와 친구인지 바로 보이도록 함께 내려준다. */
    val friends: List<AdminMemberSummaryDto>,
    /** 이 회원의 직원 권한. 비어 있으면 직원이 아니다. */
    val staffPermissions: List<StaffPermission> = emptyList(),
    /** 서비스별 처음·마지막 이용 시각(UTC, CHAT, COMMERCE 순). 비어 있으면 아직 이용 기록이 없다. */
    val services: List<ServiceUsageDto> = emptyList(),
    /** 회원 상태(ACTIVE | WITHDRAWN). */
    val status: MemberStatus = MemberStatus.ACTIVE,
) {
    companion object {
        fun of(r: AdminMemberDetailResult) = AdminMemberDetailDto(
            MemberDto.of(r.member), r.friendCount, r.createdDate, r.friends.map(AdminMemberSummaryDto::of),
            r.staffPermissions, r.services.map(ServiceUsageDto::of), r.status,
        )
    }
}
