package com.example.memberservice.api.admin

import com.example.memberservice.application.usecase.MemberAdminUseCase
import com.example.memberservice.application.usecase.command.UpdateProfileCommand
import com.example.memberservice.application.usecase.result.AdminFriendPageResult
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import com.example.memberservice.application.usecase.result.MemberResult
import com.example.memberservice.application.usecase.result.ServiceUsageResult
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.repository.query.AdminFriendFilter
import com.example.memberservice.application.domain.repository.query.FriendCounts
import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.query.ServiceFilter
import java.time.LocalDateTime
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class MemberAdminControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberService: MemberAdminUseCase

    private fun member(username: String) = MemberResult(null, null, null, null, null, username, null, null, null)

    private fun detail(
        member: MemberResult,
        friendCount: Int,
        friends: List<AdminMemberSummaryResult> = listOf(),
        services: List<ServiceUsageResult> = listOf(),
        status: MemberStatus = MemberStatus.ACTIVE,
    ) = AdminMemberDetailResult(member, friendCount, LocalDateTime.now(), friends, listOf(), services, status)

    @Test
    fun withoutToken_is403() {
        mockMvc.perform(get("/api-admin/member")).andExpect(status().isForbidden)
    }

    @Test
    fun search_returnsPage() {
        val dto = AdminMemberSummaryResult(1L, "u1", "Alice", "profile.jpg", "a@b.c", Role.ROLE_MEMBER, LocalDateTime.now(), null)
        whenever(memberService.searchMembers(eq("a"), anyOrNull(), any(), anyOrNull())).thenReturn(PageImpl(listOf(dto)))

        mockMvc.perform(get("/api-admin/member").param("keyword", "a").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].username").value("Alice"))
            .andExpect(jsonPath("$.content[0].profileImage").value("profile.jpg"))
            .andExpect(jsonPath("$.content[0].email").value("a@b.c"))
            .andExpect(jsonPath("$.content[0].createdDate").value(matchesPattern("^\\d{4}-.*")))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    /** 정렬을 안 주면 이름 가나다순(한글 먼저)이다. 예전 기본값이던 최신 가입순이 아니다. */
    @Test
    fun list_defaultsToNameAsc() {
        whenever(memberService.searchMembers(eq("a"), anyOrNull(), any(), anyOrNull())).thenReturn(PageImpl(listOf()))
        mockMvc.perform(get("/api-admin/member").param("keyword", "a").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)

        val sortCaptor = argumentCaptor<MemberSort>()
        val pageableCaptor = argumentCaptor<org.springframework.data.domain.Pageable>()
        verify(memberService).searchMembers(eq("a"), sortCaptor.capture(), pageableCaptor.capture(), anyOrNull())
        assertEquals(MemberSort.NAME_ASC, sortCaptor.firstValue)
        // 한글 우선 규칙은 Sort 로 못 담아 QueryDSL 이 만든다. Pageable 에는 정렬을 싣지 않는다.
        assertEquals(Sort.unsorted(), pageableCaptor.firstValue.sort)
    }

    @Test
    fun list_acceptsEachAllowedSort() {
        whenever(memberService.searchMembers(anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(PageImpl(listOf()))

        assertSortParsedAs("name,asc", MemberSort.NAME_ASC)
        assertSortParsedAs("name,desc", MemberSort.NAME_DESC)
        assertSortParsedAs("email,asc", MemberSort.EMAIL_ASC)
        assertSortParsedAs("email,desc", MemberSort.EMAIL_DESC)
        assertSortParsedAs("userId,asc", MemberSort.USER_ID_ASC)
        assertSortParsedAs("userId,desc", MemberSort.USER_ID_DESC)
        assertSortParsedAs("role,asc", MemberSort.ROLE_ASC)
        assertSortParsedAs("role,desc", MemberSort.ROLE_DESC)
        assertSortParsedAs("createdDate,desc", MemberSort.CREATED_DESC)
        assertSortParsedAs("createdDate,asc", MemberSort.CREATED_ASC)
    }

    private fun assertSortParsedAs(param: String, expected: MemberSort) {
        clearInvocations(memberService)
        mockMvc.perform(get("/api-admin/member").param("sort", param).header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
        val captor = argumentCaptor<MemberSort>()
        verify(memberService).searchMembers(anyOrNull(), captor.capture(), any(), anyOrNull())
        assertEquals(expected, captor.firstValue)
    }

    @Test
    fun list_rejectsUnknownSort() {
        // 목록에 값이 보이지 않는 열은 정렬할 수 없다.
        mockMvc.perform(get("/api-admin/member").param("sort", "statusMessage,asc").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/api-admin/member").param("sort", "name,sideways").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)

        verify(memberService, never()).searchMembers(anyOrNull(), anyOrNull(), any(), anyOrNull())
    }

    @Test
    fun me_withoutUserIdHeader_is401() {
        mockMvc.perform(get("/api-admin/member/me").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun me_withUserIdHeader_returnsSelf() {
        val member = member("Alice")
        val dto = detail(member, 3)
        whenever(memberService.getMe("admin")).thenReturn(dto)

        mockMvc.perform(
            get("/api-admin/member/me")
                .header("X-Internal-Token", "test-internal-token")
                .header("X-Auth-User-Id", "admin"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.member.username").value("Alice"))

        verify(memberService, never()).getMemberDetail(any())
    }

    @Test
    fun updateMe_withoutUserIdHeader_is401() {
        mockMvc.perform(
            put("/api-admin/member/me")
                .header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun updateMe_withUserIdHeader_returnsUpdatedSelf() {
        val member = member("Bob")
        val dto = detail(member, 2)
        whenever(memberService.updateMe(eq("admin"), any<UpdateProfileCommand>())).thenReturn(dto)

        mockMvc.perform(
            put("/api-admin/member/me")
                .header("X-Internal-Token", "test-internal-token")
                .header("X-Auth-User-Id", "admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Bob\"}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.member.username").value("Bob"))
    }

    @Test
    fun detail_includesFriendList() {
        val member = member("Alice")
        val friend = AdminMemberSummaryResult(
            50L, "demo-jiwoo", "김지우", "a.png", "jiwoo@modu.chat", Role.ROLE_MEMBER, LocalDateTime.now(), "지우야",
        )
        whenever(memberService.getMemberDetail(1L))
            .thenReturn(detail(member, 1, listOf(friend)))

        mockMvc.perform(get("/api-admin/member/1").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.friendCount").value(1))
            .andExpect(jsonPath("$.friends[0].username").value("김지우"))
            .andExpect(jsonPath("$.friends[0].userId").value("demo-jiwoo"))
            .andExpect(jsonPath("$.friends[0].friendName").value("지우야"))
    }
    @Test
    fun list_passesEachServiceFilter() {
        whenever(memberService.searchMembers(anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(PageImpl(listOf()))

        for (filter in ServiceFilter.entries) {
            clearInvocations(memberService)
            mockMvc.perform(get("/api-admin/member").param("service", filter.name).header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk)
            verify(memberService).searchMembers(anyOrNull(), anyOrNull(), any(), eq(filter))
        }

        clearInvocations(memberService)
        mockMvc.perform(get("/api-admin/member").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
        verify(memberService).searchMembers(anyOrNull(), anyOrNull(), any(), eq(null))
    }

    @Test
    fun list_rejectsUnknownServiceFilter() {
        mockMvc.perform(get("/api-admin/member").param("service", "SHOP").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)
        verify(memberService, never()).searchMembers(anyOrNull(), anyOrNull(), any(), anyOrNull())
    }

    @Test
    fun list_itemsCarryServices() {
        val dto = AdminMemberSummaryResult(
            1L, "u1", "Alice", null, "a@b.c", Role.ROLE_MEMBER, LocalDateTime.now(), null,
            services = listOf(ModuService.CHAT, ModuService.COMMERCE), status = MemberStatus.WITHDRAWN,
        )
        whenever(memberService.searchMembers(anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(PageImpl(listOf(dto)))

        mockMvc.perform(get("/api-admin/member").param("service", "BOTH").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].services[0]").value("CHAT"))
            .andExpect(jsonPath("$.content[0].services[1]").value("COMMERCE"))
            .andExpect(jsonPath("$.content[0].status").value("WITHDRAWN"))
    }

    @Test
    fun detail_carriesServiceUsage() {
        val usage = ServiceUsageResult(
            ModuService.CHAT, LocalDateTime.parse("2026-09-01T01:02:03"), LocalDateTime.parse("2026-09-20T04:05:06"),
        )
        whenever(memberService.getMemberDetail(1L))
            .thenReturn(detail(member("Alice"), 0, services = listOf(usage), status = MemberStatus.ACTIVE))

        mockMvc.perform(get("/api-admin/member/1").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.services[0].service").value("CHAT"))
            .andExpect(jsonPath("$.services[0].firstUsedAt").value("2026-09-01T01:02:03"))
            .andExpect(jsonPath("$.services[0].lastUsedAt").value("2026-09-20T04:05:06"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    private fun emptyFriendPage() = AdminFriendPageResult(listOf(), 0, 0, 0, 10, FriendCounts())

    @Test
    fun friends_defaultsToAllFirstPageOfTen() {
        whenever(memberService.getMemberFriends(eq(1L), any(), any())).thenReturn(emptyFriendPage())

        mockMvc.perform(get("/api-admin/member/1/friends").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.counts.all").value(0))

        val filterCaptor = argumentCaptor<AdminFriendFilter>()
        val pageableCaptor = argumentCaptor<org.springframework.data.domain.Pageable>()
        verify(memberService).getMemberFriends(eq(1L), filterCaptor.capture(), pageableCaptor.capture())
        assertEquals(AdminFriendFilter.ALL, filterCaptor.firstValue)
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(10, pageableCaptor.firstValue.pageSize)
    }

    @Test
    fun friends_passesEachFilterAndCapsSizeAt50() {
        whenever(memberService.getMemberFriends(any(), any(), any())).thenReturn(emptyFriendPage())

        for (filter in AdminFriendFilter.entries) {
            clearInvocations(memberService)
            mockMvc.perform(
                get("/api-admin/member/1/friends").param("filter", filter.name).param("page", "2").param("size", "500")
                    .header("X-Internal-Token", "test-internal-token"),
            ).andExpect(status().isOk)
            val pageableCaptor = argumentCaptor<org.springframework.data.domain.Pageable>()
            verify(memberService).getMemberFriends(eq(1L), eq(filter), pageableCaptor.capture())
            assertEquals(2, pageableCaptor.firstValue.pageNumber)
            assertEquals(50, pageableCaptor.firstValue.pageSize)
        }
    }

    @Test
    fun friends_rejectsUnknownFilter() {
        mockMvc.perform(get("/api-admin/member/1/friends").param("filter", "DELETED").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)
        verify(memberService, never()).getMemberFriends(any(), any(), any())
    }
}
