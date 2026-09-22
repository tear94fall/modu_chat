package com.example.memberservice.api.admin

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.dto.UpdateProfileDto
import com.example.memberservice.member.entity.Role
import com.example.memberservice.member.repository.MemberSort
import com.example.memberservice.member.service.MemberService
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
    @MockitoBean lateinit var memberService: MemberService

    @Test
    fun withoutToken_is403() {
        mockMvc.perform(get("/api-admin/member")).andExpect(status().isForbidden)
    }

    @Test
    fun search_returnsPage() {
        val dto = AdminMemberSummaryDto(1L, "u1", "Alice", "profile.jpg", "a@b.c", Role.ROLE_MEMBER, LocalDateTime.now(), null)
        whenever(memberService.searchMembers(eq("a"), anyOrNull(), any())).thenReturn(PageImpl(listOf(dto)))

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
        whenever(memberService.searchMembers(eq("a"), anyOrNull(), any())).thenReturn(PageImpl(listOf()))
        mockMvc.perform(get("/api-admin/member").param("keyword", "a").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)

        val sortCaptor = argumentCaptor<MemberSort>()
        val pageableCaptor = argumentCaptor<org.springframework.data.domain.Pageable>()
        verify(memberService).searchMembers(eq("a"), sortCaptor.capture(), pageableCaptor.capture())
        assertEquals(MemberSort.NAME_ASC, sortCaptor.firstValue)
        // 한글 우선 규칙은 Sort 로 못 담아 QueryDSL 이 만든다. Pageable 에는 정렬을 싣지 않는다.
        assertEquals(Sort.unsorted(), pageableCaptor.firstValue.sort)
    }

    @Test
    fun list_acceptsEachAllowedSort() {
        whenever(memberService.searchMembers(anyOrNull(), anyOrNull(), any())).thenReturn(PageImpl(listOf()))

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
        verify(memberService).searchMembers(anyOrNull(), captor.capture(), any())
        assertEquals(expected, captor.firstValue)
    }

    @Test
    fun list_rejectsUnknownSort() {
        // 목록에 값이 보이지 않는 열은 정렬할 수 없다.
        mockMvc.perform(get("/api-admin/member").param("sort", "statusMessage,asc").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/api-admin/member").param("sort", "name,sideways").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isBadRequest)

        verify(memberService, never()).searchMembers(anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun me_withoutUserIdHeader_is401() {
        mockMvc.perform(get("/api-admin/member/me").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun me_withUserIdHeader_returnsSelf() {
        val member = MemberDto(username = "Alice")
        val dto = AdminMemberDetailDto(member, 3, LocalDateTime.now(), listOf())
        whenever(memberService.getMemberDetailByUserId("admin")).thenReturn(dto)

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
        val member = MemberDto(username = "Bob")
        val dto = AdminMemberDetailDto(member, 2, LocalDateTime.now(), listOf())
        whenever(memberService.updateMyProfile(eq("admin"), any<UpdateProfileDto>())).thenReturn(dto)

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
        val member = MemberDto(username = "Alice")
        val friend = AdminMemberSummaryDto(
            50L, "demo-jiwoo", "김지우", "a.png", "jiwoo@modu.chat", Role.ROLE_MEMBER, LocalDateTime.now(), "지우야",
        )
        whenever(memberService.getMemberDetail(1L))
            .thenReturn(AdminMemberDetailDto(member, 1, LocalDateTime.now(), listOf(friend)))

        mockMvc.perform(get("/api-admin/member/1").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.friendCount").value(1))
            .andExpect(jsonPath("$.friends[0].username").value("김지우"))
            .andExpect(jsonPath("$.friends[0].userId").value("demo-jiwoo"))
            .andExpect(jsonPath("$.friends[0].friendName").value("지우야"))
    }
}
