package com.example.chatservice.api.pub

import com.example.chatservice.chat.service.ChatRoomService
import com.example.chatservice.chat.service.ChatService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 게이트웨이가 넣어 주는 X-Auth-User-Id 가 요청자로 서비스까지 내려가는지,
 * 없으면 null(=필터 없음)로 내려가는지 본다. 실제 제외 규칙은 서비스·저장소 테스트가 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatPublicBlockHeaderTest {

    companion object {
        private const val HEADER = "X-Auth-User-Id"
        private const val ME = "google-sub-me"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var chatService: ChatService
    @MockitoBean lateinit var chatRoomService: ChatRoomService

    @Test
    fun 이력_엔드포인트들은_헤더의_요청자를_넘긴다() {
        whenever(chatService.searchChatByRoomIdSize(any(), any(), anyOrNull())).thenReturn(listOf())
        whenever(chatService.searchPrevChatByRoomId(any(), any(), any(), anyOrNull())).thenReturn(listOf())
        whenever(chatService.searchImageChatByRoomIdSize(any(), any(), anyOrNull())).thenReturn(listOf())
        whenever(chatService.searchChatByRoomId(any(), anyOrNull())).thenReturn(listOf())
        whenever(chatService.searchChatListById(any(), anyOrNull())).thenReturn(listOf())

        mockMvc.perform(get("/api-public/chat/r1/page/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/100/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/images/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/chats").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat").param("ids", "1", "2").header(HEADER, ME)).andExpect(status().isOk)

        verify(chatService).searchChatByRoomIdSize("r1", "30", ME)
        verify(chatService).searchPrevChatByRoomId("r1", "100", "30", ME)
        verify(chatService).searchImageChatByRoomIdSize("r1", "30", ME)
        verify(chatService).searchChatByRoomId("r1", ME)
        verify(chatService).searchChatListById(listOf("1", "2"), ME)
    }

    @Test
    fun 헤더가_없으면_요청자가_없다고_내려간다() {
        whenever(chatService.searchChatByRoomIdSize(any(), any(), anyOrNull())).thenReturn(listOf())

        mockMvc.perform(get("/api-public/chat/r1/page/30")).andExpect(status().isOk)

        verify(chatService).searchChatByRoomIdSize(eq("r1"), eq("30"), isNull())
    }

    @Test
    fun 미읽음은_경로의_memberId_와_헤더의_userId_를_함께_넘긴다() {
        whenever(chatRoomService.searchUnreadChatRoom(any(), anyOrNull())).thenReturn(listOf())

        mockMvc.perform(get("/api-public/chat/unread/7").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/unread/7")).andExpect(status().isOk)

        verify(chatRoomService).searchUnreadChatRoom("7", ME)
        verify(chatRoomService).searchUnreadChatRoom(eq("7"), isNull())
    }
}
