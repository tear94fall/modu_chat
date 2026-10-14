package com.example.memberservice.application.service

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.query.ServiceFilter
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberServiceUsageRwRepository
import com.example.memberservice.application.support.ApplicationTestSupport
import com.example.memberservice.application.domain.repository.query.MemberSort
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

/** 백오피스 회원 목록의 service 필터와 목록·상세의 services·status 필드. 표식으로 이 테스트의 회원만 본다. */
class MemberAdminServiceFilterTest : ApplicationTestSupport() {

    companion object {
        private const val MARK = "svcfilter"
        private val T1: LocalDateTime = LocalDateTime.parse("2026-09-01T01:02:03")
        private val T2: LocalDateTime = LocalDateTime.parse("2026-09-20T04:05:06")
    }

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberRoRepository: MemberRoRepository
    @Autowired lateinit var usageRepository: MemberServiceUsageRwRepository
    @Autowired lateinit var memberService: MemberQueryService

    private lateinit var chatOnly: Member
    private lateinit var commerceOnly: Member
    private lateinit var both: Member
    private lateinit var none: Member

    private fun member(id: String): Member = memberRepository.save(
        Member(userId = "$MARK-$id", email = "$MARK-$id@example.com", username = id, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    @BeforeEach
    fun setUp() {
        chatOnly = member("a-chat")
        commerceOnly = member("b-commerce")
        both = member("c-both")
        none = member("d-none")
        usageRepository.save(MemberServiceUsage(chatOnly.userId, ModuService.CHAT, T1))
        usageRepository.save(MemberServiceUsage(commerceOnly.userId, ModuService.COMMERCE, T1))
        // 저장 순서를 COMMERCE 먼저로 해도 응답은 CHAT, COMMERCE 순이어야 한다.
        usageRepository.save(MemberServiceUsage(both.userId, ModuService.COMMERCE, T2))
        usageRepository.save(MemberServiceUsage(both.userId, ModuService.CHAT, T1, T2))
    }

    private fun userIds(filter: ServiceFilter?) =
        memberRoRepository.searchForAdmin(MARK, MemberSort.USER_ID_ASC, Pageable.unpaged(), service = filter).content.map { it.userId }

    @Test
    fun 필터마다_해당_회원만_남는다() {
        assertThat(userIds(null)).containsExactly(chatOnly.userId, commerceOnly.userId, both.userId, none.userId)
        assertThat(userIds(ServiceFilter.CHAT)).containsExactly(chatOnly.userId, both.userId)
        assertThat(userIds(ServiceFilter.COMMERCE)).containsExactly(commerceOnly.userId, both.userId)
        assertThat(userIds(ServiceFilter.BOTH)).containsExactly(both.userId)
        assertThat(userIds(ServiceFilter.NONE)).containsExactly(none.userId)
    }

    @Test
    fun 필터가_있어도_페이지_합계가_맞다() {
        val page = memberRoRepository.searchForAdmin(MARK, MemberSort.USER_ID_ASC, PageRequest.of(0, 1), service = ServiceFilter.CHAT)
        assertThat(page.totalElements).isEqualTo(2)
        assertThat(page.content.map { it.userId }).containsExactly(chatOnly.userId)
    }

    @Test
    fun 목록_항목에_이용_서비스가_CHAT_COMMERCE_순으로_붙는다() {
        val page = memberService.searchMembers(MARK, MemberSort.USER_ID_ASC, PageRequest.of(0, 10))
        val services = page.content.associate { it.userId to it.services }

        assertThat(services[chatOnly.userId]).containsExactly(ModuService.CHAT)
        assertThat(services[commerceOnly.userId]).containsExactly(ModuService.COMMERCE)
        assertThat(services[both.userId]).containsExactly(ModuService.CHAT, ModuService.COMMERCE)
        assertThat(services[none.userId]).isEmpty()

        val filtered = memberService.searchMembers(MARK, MemberSort.USER_ID_ASC, PageRequest.of(0, 10), ServiceFilter.BOTH)
        assertThat(filtered.content.map { it.userId }).containsExactly(both.userId)
    }

    @Test
    fun 상세에_서비스별_처음_마지막_이용_시각이_붙는다() {
        val detail = memberService.getMemberDetail(both.id!!)
        assertThat(detail.services.map { it.service }).containsExactly(ModuService.CHAT, ModuService.COMMERCE)
        assertThat(detail.services[0].firstUsedAt).isEqualTo(T1)
        assertThat(detail.services[0].lastUsedAt).isEqualTo(T2)
        assertThat(detail.services[1].firstUsedAt).isEqualTo(T2)

        assertThat(memberService.getMemberDetail(none.id!!).services).isEmpty()
    }

    @Test
    fun 목록과_상세에_회원_상태가_붙는다() {
        none.withdraw()
        memberRepository.save(none)

        val statuses = memberService.searchMembers(MARK, MemberSort.USER_ID_ASC, PageRequest.of(0, 10)).content
            .associate { it.userId to it.status }
        assertThat(statuses[chatOnly.userId]).isEqualTo(MemberStatus.ACTIVE)
        assertThat(statuses[none.userId]).isEqualTo(MemberStatus.WITHDRAWN)

        assertThat(memberService.getMemberDetail(chatOnly.id!!).status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(memberService.getMemberDetail(none.id!!).status).isEqualTo(MemberStatus.WITHDRAWN)
    }
}
