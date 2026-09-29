package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.service.StaffCommandService
import com.example.memberservice.application.service.StaffQueryService
import com.example.memberservice.application.usecase.command.SetStaffPermissionsCommand
import com.example.memberservice.application.usecase.result.StaffEntryResult
import com.example.memberservice.application.usecase.result.StaffLoginResult
import com.example.memberservice.application.usecase.result.StaffMemberDetailResult
import com.example.memberservice.application.usecase.result.StaffMemberSummaryResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 직원 유스케이스. 로그인·토큰 갱신(auth-service), 직원 목록, 회원 상세는 권한을 바꾼 직후에 읽히므로 master,
 * 회원 목록은 둘러보기라 레플리카.
 */
@Component
class StaffUseCase(
    private val staffQueryService: StaffQueryService,
    private val staffCommandService: StaffCommandService,
) {

    fun loginByEmail(email: String): StaffLoginResult? = staffCommandService.loginByEmail(email)

    fun loginByUserId(userId: String): StaffLoginResult? = staffCommandService.loginByUserId(userId)

    fun searchMembers(keyword: String?, sort: MemberSort, pageable: Pageable, staffOnly: Boolean): Page<StaffMemberSummaryResult> =
        staffQueryService.searchMembers(keyword, sort, pageable, staffOnly)

    fun memberDetail(id: Long): StaffMemberDetailResult = staffCommandService.memberDetail(id)

    fun list(): List<StaffEntryResult> = staffCommandService.list()

    fun setPermissions(command: SetStaffPermissionsCommand): StaffEntryResult = staffCommandService.setPermissions(command)

    fun remove(actorUserId: String, memberId: Long) = staffCommandService.remove(actorUserId, memberId)
}
