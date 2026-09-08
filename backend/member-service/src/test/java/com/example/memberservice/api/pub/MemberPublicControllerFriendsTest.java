package com.example.memberservice.api.pub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.repository.FriendSort;
import com.example.memberservice.member.service.MemberService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberPublicControllerFriendsTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MemberService memberService;

    @Test
    void 친구_목록은_page_size_로_잘라_페이지_응답으로_돌려준다() throws Exception {
        MemberDto friend = MemberDto.builder().id(7L).userId("f1").username("강감찬").email("kang@example.com").build();
        when(memberService.getFriendsPage(eq("me"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(friend), PageRequest.of(1, 20), 21));

        mockMvc.perform(get("/api-public/member/me/friends").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("강감찬"))
                .andExpect(jsonPath("$.content[0].email").value("kang@example.com"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void page_size_가_없으면_기본값이고_size_는_100을_넘지_않는다() throws Exception {
        when(memberService.getFriendsPage(eq("me"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        mockMvc.perform(get("/api-public/member/me/friends")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("page", "-3").param("size", "500")).andExpect(status().isOk());

        verify(memberService, org.mockito.Mockito.times(2)).getFriendsPage(eq("me"), eq(FriendSort.NAME_ASC), pageable.capture());
        assertThat(pageable.getAllValues().get(0).getPageNumber()).isEqualTo(0);
        assertThat(pageable.getAllValues().get(0).getPageSize()).isEqualTo(50);
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isEqualTo(0);
        assertThat(pageable.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    @Test
    void sort_파라미터는_허용_목록으로_해석하고_모르는_값은_400이다() throws Exception {
        when(memberService.getFriendsPage(eq("me"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "name,desc")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "email,asc")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "createdDate,desc")).andExpect(status().isBadRequest());

        verify(memberService).getFriendsPage(eq("me"), eq(FriendSort.NAME_DESC), any());
        verify(memberService).getFriendsPage(eq("me"), eq(FriendSort.EMAIL_ASC), any());
    }
}
