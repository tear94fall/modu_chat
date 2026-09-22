package com.example.memberservice.api.pub

import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.global.exception.ErrorCode
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.dto.ResponseFriendDto
import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.repository.FriendFilter
import com.example.memberservice.member.repository.FriendSort
import com.example.memberservice.member.service.MemberFriendService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.modelmapper.ModelMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@AutoConfigureMockMvc
class MemberPublicControllerFriendsTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberFriendService: MemberFriendService

    private fun friends(): String = "/api-public/member/me/friends"

    @Test
    fun 친구_목록은_page_size_로_잘라_페이지_응답으로_돌려준다() {
        val friend = ResponseFriendDto(id = 7L, userId = "f1", username = "강감찬", email = "kang@example.com", friendName = "감찬이")
        whenever(memberFriendService.getFriendsPage(eq("me"), anyOrNull(), any(), any()))
            .thenReturn(PageImpl(listOf(friend), PageRequest.of(1, 20), 21))

        mockMvc.perform(get(friends()).param("page", "1").param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].username").value("강감찬"))
            .andExpect(jsonPath("$.content[0].email").value("kang@example.com"))
            .andExpect(jsonPath("$.content[0].friendName").value("감찬이"))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(21))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.last").value(true))
    }

    @Test
    fun page_size_가_없으면_기본값이고_size_는_100을_넘지_않는다() {
        whenever(memberFriendService.getFriendsPage(eq("me"), anyOrNull(), any(), any()))
            .thenReturn(PageImpl(listOf(), PageRequest.of(0, 50), 0))
        val pageable = argumentCaptor<Pageable>()

        mockMvc.perform(get(friends())).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("page", "-3").param("size", "500")).andExpect(status().isOk)

        verify(memberFriendService, times(2)).getFriendsPage(eq("me"), eq(FriendFilter.NORMAL), eq(FriendSort.NAME_ASC), pageable.capture())
        assertThat(pageable.allValues[0].pageNumber).isEqualTo(0)
        assertThat(pageable.allValues[0].pageSize).isEqualTo(50)
        assertThat(pageable.allValues[1].pageNumber).isEqualTo(0)
        assertThat(pageable.allValues[1].pageSize).isEqualTo(100)
    }

    @Test
    fun sort_파라미터는_허용_목록으로_해석하고_모르는_값은_400이다() {
        whenever(memberFriendService.getFriendsPage(eq("me"), anyOrNull(), any(), any()))
            .thenReturn(PageImpl(listOf(), PageRequest.of(0, 50), 0))

        mockMvc.perform(get(friends()).param("sort", "name,desc")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("sort", "email,asc")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("sort", "createdDate,desc")).andExpect(status().isBadRequest)

        verify(memberFriendService).getFriendsPage(eq("me"), anyOrNull(), eq(FriendSort.NAME_DESC), any())
        verify(memberFriendService).getFriendsPage(eq("me"), anyOrNull(), eq(FriendSort.EMAIL_ASC), any())
    }

    @Test
    fun 이름_맵을_돌려준다() {
        whenever(memberFriendService.getFriendNames("me")).thenReturn(mapOf("f1" to "감찬이", "f2" to ""))

        mockMvc.perform(get("/api-public/member/me/friends/names"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.f1").value("감찬이"))
            .andExpect(jsonPath("$.f2").value(""))
    }

    @Test
    fun 별칭_변경은_공백이면_400_친구가_아니면_404_정상이면_바뀐_값을_돌려준다() {
        whenever(memberFriendService.renameFriend("me", 7L, "감찬이"))
            .thenReturn(ResponseFriendDto(id = 7L, friendName = "감찬이"))
        whenever(memberFriendService.renameFriend(eq("me"), eq(9L), any()))
            .thenThrow(CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"))

        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  감찬이 \"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.friendName").value("감찬이"))
        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(put("/api-public/member/me/friends/7/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "가".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(put("/api-public/member/me/friends/9/name").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun filter_파라미터는_허용_목록으로_해석하고_모르는_값은_400이다() {
        whenever(memberFriendService.getFriendsPage(eq("me"), anyOrNull(), any(), any()))
            .thenReturn(PageImpl(listOf(), PageRequest.of(0, 50), 0))

        mockMvc.perform(get(friends()).param("filter", "normal")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("filter", "favorite")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("filter", "hidden")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("filter", "blocked")).andExpect(status().isOk)
        mockMvc.perform(get(friends()).param("filter", "deleted")).andExpect(status().isBadRequest)

        // 잘못된 filter 는 서비스까지 가지 않는다
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.NORMAL), any(), any())
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.FAVORITE), any(), any())
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.HIDDEN), any(), any())
        verify(memberFriendService).getFriendsPage(eq("me"), eq(FriendFilter.BLOCKED), any(), any())
    }

    @Test
    fun 목록_항목에는_즐겨찾기와_상태가_실린다() {
        val friend = ResponseFriendDto(id = 7L, userId = "f1", username = "강감찬", favorite = true, status = FriendStatus.NORMAL)
        whenever(memberFriendService.getFriendsPage(eq("me"), anyOrNull(), any(), any()))
            .thenReturn(PageImpl(listOf(friend), PageRequest.of(0, 50), 1))

        mockMvc.perform(get(friends()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].favorite").value(true))
            .andExpect(jsonPath("$.content[0].status").value("NORMAL"))
    }

    @Test
    fun 친구_한_명은_상태와_함께_돌려주고_친구가_아니면_404다() {
        whenever(memberFriendService.getFriend("me", 7L))
            .thenReturn(ResponseFriendDto(id = 7L, userId = "f1", favorite = true, status = FriendStatus.HIDDEN))
        whenever(memberFriendService.getFriend("me", 9L)).thenThrow(CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"))

        mockMvc.perform(get("/api-public/member/me/friends/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.favorite").value(true))
            .andExpect(jsonPath("$.status").value("HIDDEN"))
        mockMvc.perform(get("/api-public/member/me/friends/9")).andExpect(status().isNotFound)
    }

    @Test
    fun 즐겨찾기는_on_off_를_그대로_넘기고_차단된_친구면_400이다() {
        whenever(memberFriendService.setFavorite("me", 7L, true))
            .thenReturn(ResponseFriendDto(id = 7L, favorite = true, status = FriendStatus.NORMAL))
        whenever(memberFriendService.setFavorite("me", 7L, false))
            .thenReturn(ResponseFriendDto(id = 7L, favorite = false, status = FriendStatus.NORMAL))
        whenever(memberFriendService.setFavorite("me", 8L, true))
            .thenThrow(ResponseStatusException(HttpStatus.BAD_REQUEST, "차단한 친구는 즐겨찾기할 수 없습니다."))
        whenever(memberFriendService.setFavorite(eq("me"), eq(9L), any()))
            .thenThrow(CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"))

        mockMvc.perform(put("/api-public/member/me/friends/7/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.favorite").value(true))
        mockMvc.perform(put("/api-public/member/me/friends/7/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":false}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.favorite").value(false))
        mockMvc.perform(put("/api-public/member/me/friends/8/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(put("/api-public/member/me/friends/9/favorite").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun 숨김과_차단도_on_off_를_그대로_넘긴다() {
        whenever(memberFriendService.setHidden("me", 7L, true))
            .thenReturn(ResponseFriendDto(id = 7L, status = FriendStatus.HIDDEN))
        whenever(memberFriendService.setHidden("me", 7L, false))
            .thenReturn(ResponseFriendDto(id = 7L, status = FriendStatus.NORMAL))
        whenever(memberFriendService.setBlocked("me", 7L, true))
            .thenReturn(ResponseFriendDto(id = 7L, favorite = false, status = FriendStatus.BLOCKED))
        whenever(memberFriendService.setBlocked(eq("me"), eq(9L), any()))
            .thenThrow(CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "9"))

        mockMvc.perform(put("/api-public/member/me/friends/7/hidden").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("HIDDEN"))
        mockMvc.perform(put("/api-public/member/me/friends/7/hidden").contentType(MediaType.APPLICATION_JSON).content("{\"on\":false}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NORMAL"))
        mockMvc.perform(put("/api-public/member/me/friends/7/blocked").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("BLOCKED"))
            .andExpect(jsonPath("$.favorite").value(false))
        mockMvc.perform(put("/api-public/member/me/friends/9/blocked").contentType(MediaType.APPLICATION_JSON).content("{\"on\":true}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun 차단한_친구의_userId_목록을_돌려준다() {
        whenever(memberFriendService.getBlockedUserIds("me")).thenReturn(listOf("f1", "f2"))

        mockMvc.perform(get("/api-public/member/me/friends/blocked-ids"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("f1"))
            .andExpect(jsonPath("$[1]").value("f2"))
    }

    /** 회원 검색(/friends/{email}) 은 ModelMapper 로 MemberDto 를 옮긴다. status 필드를 늘린 뒤에도 statusMessage 와 헷갈리지 않아야 한다. */
    @Test
    fun 회원_검색_매핑은_statusMessage_를_status_로_잘못_옮기지_않는다() {
        val source = MemberDto(id = 1L, userId = "f1", email = "f1@example.com", username = "김철수", statusMessage = "안녕")

        val mapped = ModelMapper().map(source, ResponseFriendDto::class.java)

        assertThat(mapped.statusMessage).isEqualTo("안녕")
        assertThat(mapped.status).isEqualTo(FriendStatus.NORMAL)
        assertThat(mapped.favorite).isFalse()
    }
}
