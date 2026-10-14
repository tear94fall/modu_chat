package com.example.memberservice.application.service

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import com.example.memberservice.application.domain.entity.Staff
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import com.example.memberservice.application.usecase.result.MemberResult
import com.example.memberservice.application.usecase.result.ServiceUsageResult

/**
 * 백오피스 회원 상세 조립. 읽는 쪽(ro: 상세 조회, rw: 내 정보 수정 직후)이 자기 저장소로 읽은 것을 넘긴다.
 * 트랜잭션 안에서 부른다(친구 Member 는 fetch join 돼 있다).
 */
internal object AdminMemberDetails {

    /** memberId → 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL 순). 직원이 아닌 회원은 맵에 없다. */
    fun permissionsByMemberId(staff: List<Staff>): Map<Long, List<StaffPermission>> =
        staff.associate { it.memberId to it.sortedPermissions() }

    fun of(
        member: Member,
        friends: List<MemberFriend>,
        staffOf: (Collection<Long>) -> List<Staff>,
        usages: List<MemberServiceUsage>,
    ): AdminMemberDetailResult {
        val summaries = friends.map(AdminMemberSummaryResult::from)
        val staff = permissionsByMemberId(staffOf(summaries.mapNotNull { it.id } + member.id!!))
        return AdminMemberDetailResult(
            member = MemberResult.from(member),
            friendCount = summaries.size,
            createdDate = member.createdDate,
            friends = summaries.map { it.copy(staffPermissions = staff[it.id].orEmpty()) },
            staffPermissions = staff[member.id].orEmpty(),
            services = usages.sortedBy { it.service }.map(ServiceUsageResult::from),
            status = member.status,
        )
    }
}
