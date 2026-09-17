package com.example.memberservice.api.pub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memberservice.member.repository.FriendFilter;
import com.example.memberservice.member.repository.FriendSort;
import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.dto.ResponseFriendDto;
import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.service.MemberFriendService;
import java.util.List;
import java.util.Map;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
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
    @MockitoBean MemberFriendService memberFriendService;

    @Test
    void 친구_목록은_page_size_로_잘라_페이지_응답으로_돌려준다() throws Exception {
        ResponseFriendDto friend = ResponseFriendDto.builder().id(7L).userId("f1").username("강감찬").email("kang@example.com").friendName("감찬이").build();
        when(memberFriendService.getFriendsPage(eq("me"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(friend), PageRequest.of(1, 20), 21));

        mockMvc.perform(get("/api-public/member/me/friends").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("강감찬"))
                .andExpect(jsonPath("$.content[0].email").value("kang@example.com"))
                .andExpect(jsonPath("$.content[0].friendName").value("감찬이"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void page_size_가_없으면_기본값이고_size_는_100을_넘지_않는다() throws Exception {
        when(memberFriendService.getFriendsPage(eq("me"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        mockMvc.perform(get("/api-public/member/me/friends")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("page", "-3").param("size", "500")).andExpect(status().isOk());

        verify(memberFriendService, org.mockito.Mockito.times(2)).getFriendsPage(eq("me"), eq(FriendFilter.NORMAL), eq(FriendSort.NAME_ASC), pageable.capture());
        assertThat(pageable.getAllValues().get(0).getPageNumber()).isEqualTo(0);
        assertThat(pageable.getAllValues().get(0).getPageSize()).isEqualTo(50);
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isEqualTo(0);
        assertThat(pageable.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    @Test
    void sort_파라미터는_허용_목록으로_해석하고_모르는_값은_400이다() throws Exception {
        when(memberFriendService.getFriendsPage(eq("me"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "name,desc")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "email,asc")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("sort", "createdDate,desc")).andExpect(status().isBadRequest());

        verify(memberFriendService).getFriendsPage(eq("me"), any(), eq(FriendSort.NAME_DESC), any());
        verify(memberFriendService).getFriendsPage(eq("me"), any(), eq(FriendSort.EMAIL_ASC), any());
    }

    @Test
    void 이름_맵을_돌려준다() throws Exception {
        when(memberFriendService.getFriendNames("me")).thenReturn(Map.of("f1", "감찬이", "f2", ""));

        mockMvc.perform(get("/api-public/member/me/friends/names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.f1").value("감찬이"))
                .andExpect(jsonPath("$.f2").value(""));
    }

    @Test
    void 별칭_변경은_공백이면_400_친구가_아니면_404_정상이면_바뀐_값을_돌려준다() throws Exception {
        when(memberFriendService.renameFriend("me", 7L, "감찬이"))
                .thenReturn(ResponseFriendDto.builder().id(7L).friendName("감찬이").build());
        when(memberFriendService.renameFriend(eq("me"), eq(9L), any()))
                .thenThrow(new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"));

        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  감찬이 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendName").value("감찬이"));
        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "가".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api-public/member/me/friends/9/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filter_파라미터는_허용_목록으로_해석하고_모르는_값은_400이다() throws Exception {
        when(memberFriendService.getFriendsPage(eq("me"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api-public/member/me/friends").param("filter", "normal")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("filter", "favorite")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("filter", "hidden")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("filter", "blocked")).andExpect(status().isOk());
        mockMvc.perform(get("/api-public/member/me/friends").param("filter", "deleted")).andExpect(status().isBadRequest());

        // 잘못된 filter 는 서비스까지 가지 않는다
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.NORMAL), any(), any());
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.FAVORITE), any(), any());
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.HIDDEN), any(), any());
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.BLOCKED), any(), any());
    }

    @Test
    void 목록_항목에는_즐겨찾기와_상태가_실린다() throws Exception {
        ResponseFriendDto friend = ResponseFriendDto.builder().id(7L).userId("f1").username("강감찬").favorite(true).status(FriendStatus.NORMAL).build();
        when(memberFriendService.getFriendsPage(eq("me"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(friend), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api-public/member/me/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].favorite").value(true))
                .andExpect(jsonPath("$.content[0].status").value("NORMAL"));
    }

    @Test
    void 친구_한_명은_상태와_함께_돌려주고_친구가_아니면_404다() throws Exception {
        when(memberFriendService.getFriend("me", 7L))
                .thenReturn(ResponseFriendDto.builder().id(7L).userId("f1").favorite(true).status(FriendStatus.HIDDEN).build());
        when(memberFriendService.getFriend("me", 9L)).thenThrow(new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"));

        mockMvc.perform(get("/api-public/member/me/friends/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mockMvc.perform(get("/api-public/member/me/friends/9")).andExpect(status().isNotFound());
    }

    @Test
    void 즐겨찾기는_on_off_를_그대로_넘기고_차단된_친구면_400이다() throws Exception {
        when(memberFriendService.setFavorite("me", 7L, true))
                .thenReturn(ResponseFriendDto.builder().id(7L).favorite(true).status(FriendStatus.NORMAL).build());
        when(memberFriendService.setFavorite("me", 7L, false))
                .thenReturn(ResponseFriendDto.builder().id(7L).favorite(false).status(FriendStatus.NORMAL).build());
        when(memberFriendService.setFavorite("me", 8L, true))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단한 친구는 즐겨찾기할 수 없습니다."));
        when(memberFriendService.setFavorite(eq("me"), eq(9L), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"));

        mockMvc.perform(put("/api-public/member/me/friends/7/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
        mockMvc.perform(put("/api-public/member/me/friends/7/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false));
        mockMvc.perform(put("/api-public/member/me/friends/8/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api-public/member/me/friends/9/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 숨김과_차단도_on_off_를_그대로_넘긴다() throws Exception {
        when(memberFriendService.setHidden("me", 7L, true))
                .thenReturn(ResponseFriendDto.builder().id(7L).status(FriendStatus.HIDDEN).build());
        when(memberFriendService.setHidden("me", 7L, false))
                .thenReturn(ResponseFriendDto.builder().id(7L).status(FriendStatus.NORMAL).build());
        when(memberFriendService.setBlocked("me", 7L, true))
                .thenReturn(ResponseFriendDto.builder().id(7L).favorite(false).status(FriendStatus.BLOCKED).build());
        when(memberFriendService.setBlocked(eq("me"), eq(9L), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"));

        mockMvc.perform(put("/api-public/member/me/friends/7/hidden").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mockMvc.perform(put("/api-public/member/me/friends/7/hidden").contentType(MediaType.APPLICATION_JSON).content("{\"on\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NORMAL"));
        mockMvc.perform(put("/api-public/member/me/friends/7/blocked").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.favorite").value(false));
        mockMvc.perform(put("/api-public/member/me/friends/9/blocked").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 차단한_친구의_userId_목록을_돌려준다() throws Exception {
        when(memberFriendService.getBlockedUserIds("me")).thenReturn(List.of("f1", "f2"));

        mockMvc.perform(get("/api-public/member/me/friends/blocked-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("f1"))
                .andExpect(jsonPath("$[1]").value("f2"));
    }

    /** 회원 검색(/friends/{email}) 은 ModelMapper 로 MemberDto 를 옮긴다. status 필드를 늘린 뒤에도 statusMessage 와 헷갈리지 않아야 한다. */
    @Test
    void 회원_검색_매핑은_statusMessage_를_status_로_잘못_옮기지_않는다() {
        MemberDto source = MemberDto.builder().id(1L).userId("f1").email("f1@example.com").username("김철수").statusMessage("안녕").build();

        ResponseFriendDto mapped = new ModelMapper().map(source, ResponseFriendDto.class);

        assertThat(mapped.getStatusMessage()).isEqualTo("안녕");
        assertThat(mapped.getStatus()).isEqualTo(FriendStatus.NORMAL);
        assertThat(mapped.isFavorite()).isFalse();
    }
}
