package com.example.pointservice.application.usecase

import com.example.pointservice.application.member.MemberLookup
import com.example.pointservice.application.member.MemberSummary
import com.example.pointservice.application.service.PointCommandService
import com.example.pointservice.application.service.PointQueryService
import com.example.pointservice.application.usecase.result.AdminPointAccountResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 백오피스 계정 화면 유스케이스. 포인트 계정(DB)에 회원 이름·이메일(member-service)을 붙인다.
 * member-service 호출은 DB 트랜잭션 밖에서 한다 — 느린 호출이 커넥션을 잡고 있지 않게.
 */
@Component
class PointAccountUseCase(
    private val pointQueryService: PointQueryService,
    private val pointCommandService: PointCommandService,
    private val memberLookup: MemberLookup,
) {

    /**
     * 계정 목록(replica). 검색어는 member-service 의 회원 검색(이름·이메일·userId)에 넘겨 걸린 회원들의 계정만 보여 준다.
     * 이름·이메일은 조회해 붙이고, member-service 가 응답하지 않으면 그 칸만 비운다.
     */
    fun accounts(keyword: String?, pageable: Pageable): Page<AdminPointAccountResult> {
        if (keyword.isNullOrBlank()) {
            val page = pointQueryService.accounts(pageable)
            val members = memberLookup.byUserIds(page.content.map { it.userId })
            return page.map { AdminPointAccountResult.of(it, members[it.userId]) }
        }
        val matched = memberLookup.search(keyword.trim())
        if (matched.isEmpty()) return Page.empty(pageable)
        val byUserId = matched.associateBy { it.userId }
        return pointQueryService.accountsOf(byUserId.keys, pageable).map { AdminPointAccountResult.of(it, byUserId[it.userId]) }
    }

    /** 계정 상세(master). 백오피스가 조정 직후 다시 읽는다. 계정이 없으면 ACCOUNT_NOT_FOUND(404). */
    fun account(userId: String): AdminPointAccountResult {
        val account = pointCommandService.account(userId)
        return AdminPointAccountResult.of(account, memberLookup.byUserIds(listOf(userId))[userId])
    }

    /** 계정이 아직 없는 사용자의 이름·이메일. 백오피스 상세가 0 P 화면에서도 누구인지 보여 주는 데 쓴다. */
    fun member(userId: String): MemberSummary? = memberLookup.byUserIds(listOf(userId))[userId]
}
