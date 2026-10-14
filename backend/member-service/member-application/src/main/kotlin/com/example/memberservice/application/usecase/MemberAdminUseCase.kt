package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.repository.query.AdminFriendFilter
import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.domain.repository.query.ServiceFilter
import com.example.memberservice.application.service.MemberCommandService
import com.example.memberservice.application.service.MemberQueryService
import com.example.memberservice.application.usecase.command.UpdateProfileCommand
import com.example.memberservice.application.usecase.result.AdminFriendPageResult
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/** 백오피스 회원 관리. 목록·상세·친구 탭은 둘러보기(레플리카), 내 정보는 수정 직후에 다시 읽으므로 master. */
@Component
class MemberAdminUseCase(
    private val memberQueryService: MemberQueryService,
    private val memberCommandService: MemberCommandService,
) {

    fun searchMembers(keyword: String?, sort: MemberSort?, pageable: Pageable, service: ServiceFilter? = null): Page<AdminMemberSummaryResult> =
        memberQueryService.searchMembers(keyword, sort, pageable, service)

    fun getMemberDetail(id: Long): AdminMemberDetailResult = memberQueryService.getMemberDetail(id)

    fun getMemberFriends(id: Long, filter: AdminFriendFilter, pageable: Pageable): AdminFriendPageResult =
        memberQueryService.getMemberFriends(id, filter, pageable)

    fun getMe(userId: String): AdminMemberDetailResult = memberCommandService.getMemberDetailByUserId(userId)

    fun updateMe(userId: String, command: UpdateProfileCommand): AdminMemberDetailResult =
        memberCommandService.updateMyProfile(userId, command)
}
