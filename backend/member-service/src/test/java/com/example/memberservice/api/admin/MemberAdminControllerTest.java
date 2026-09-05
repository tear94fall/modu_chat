package com.example.memberservice.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto;
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.dto.UpdateProfileDto;
import com.example.memberservice.member.entity.Role;
import com.example.memberservice.member.service.MemberService;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MemberService memberService;

    @Test
    void withoutToken_is403() throws Exception {
        mockMvc.perform(get("/api-admin/member")).andExpect(status().isForbidden());
    }

    @Test
    void search_returnsPage() throws Exception {
        AdminMemberSummaryDto dto = new AdminMemberSummaryDto(1L, "u1", "Alice", "profile.jpg", "a@b.c", Role.ROLE_MEMBER, LocalDateTime.now());
        when(memberService.searchMembers(eq("a"), any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api-admin/member").param("keyword", "a").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("Alice"))
                .andExpect(jsonPath("$.content[0].profileImage").value("profile.jpg"))
                .andExpect(jsonPath("$.content[0].email").value("a@b.c"))
                .andExpect(jsonPath("$.content[0].createdDate").value(matchesPattern("^\\d{4}-.*")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_sortsNewestFirst() throws Exception {
        when(memberService.searchMembers(eq("a"), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api-admin/member").param("keyword", "a").header("X-Internal-Token", "test-internal-token")).andExpect(status().isOk());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(memberService).searchMembers(eq("a"), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdDate").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
    }

    @Test
    void me_withoutUserIdHeader_is401() throws Exception {
        mockMvc.perform(get("/api-admin/member/me").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withUserIdHeader_returnsSelf() throws Exception {
        MemberDto member = MemberDto.builder().username("Alice").build();
        AdminMemberDetailDto dto = new AdminMemberDetailDto(member, 3, LocalDateTime.now(), List.of());
        when(memberService.getMemberDetailByUserId("admin")).thenReturn(dto);

        mockMvc.perform(get("/api-admin/member/me")
                        .header("X-Internal-Token", "test-internal-token")
                        .header("X-Auth-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.username").value("Alice"));

        verify(memberService, never()).getMemberDetail(any());
    }

    @Test
    void updateMe_withoutUserIdHeader_is401() throws Exception {
        mockMvc.perform(put("/api-admin/member/me")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_withUserIdHeader_returnsUpdatedSelf() throws Exception {
        MemberDto member = MemberDto.builder().username("Bob").build();
        AdminMemberDetailDto dto = new AdminMemberDetailDto(member, 2, LocalDateTime.now(), List.of());
        when(memberService.updateMyProfile(eq("admin"), any(UpdateProfileDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api-admin/member/me")
                        .header("X-Internal-Token", "test-internal-token")
                        .header("X-Auth-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.username").value("Bob"));
    }

    @Test
    void detail_includesFriendList() throws Exception {
        MemberDto member = MemberDto.builder().username("Alice").build();
        AdminMemberSummaryDto friend = new AdminMemberSummaryDto(
                50L, "demo-jiwoo", "김지우", "a.png", "jiwoo@modu.chat", Role.ROLE_MEMBER, LocalDateTime.now());
        when(memberService.getMemberDetail(1L))
                .thenReturn(new AdminMemberDetailDto(member, 1, LocalDateTime.now(), List.of(friend)));

        mockMvc.perform(get("/api-admin/member/1").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendCount").value(1))
                .andExpect(jsonPath("$.friends[0].username").value("김지우"))
                .andExpect(jsonPath("$.friends[0].userId").value("demo-jiwoo"));
    }

}
