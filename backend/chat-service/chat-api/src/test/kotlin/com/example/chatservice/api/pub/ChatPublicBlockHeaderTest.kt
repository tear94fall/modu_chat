package com.example.chatservice.api.pub

import com.example.chatservice.application.usecase.ChatRoomUseCase
import com.example.chatservice.application.usecase.ChatUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 게이트웨이가 넣어 주는 X-Auth-User-Id 가 요청자로 유스케이스까지 내려가는지,
 * 없으면 유스케이스에 닿기 전에 403 인지 본다. 실제 제외·본인 확인 규칙은 유스케이스·저장소 테스트가 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatPublicBlockHeaderTest {

    companion object {
        private const val HEADER = "X-Auth-User-Id"
        private const val ME = "google-sub-me"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var chatUseCase: ChatUseCase
    @MockitoBean lateinit var chatRoomUseCase: ChatRoomUseCase

    @Test
    fun 이력_엔드포인트들은_헤더의_요청자를_넘긴다() {
        whenever(chatUseCase.recent(anyOrNull(), any(), any())).thenReturn(listOf())
        whenever(chatUseCase.before(anyOrNull(), any(), any(), any())).thenReturn(listOf())
        whenever(chatUseCase.images(anyOrNull(), any(), any())).thenReturn(listOf())
        whenever(chatUseCase.history(anyOrNull(), any())).thenReturn(listOf())
        whenever(chatUseCase.chats(anyOrNull(), any())).thenReturn(listOf())

        mockMvc.perform(get("/api-public/chat/r1/page/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/100/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/images/30").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/r1/chats").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat").param("ids", "1", "2").header(HEADER, ME)).andExpect(status().isOk)

        verify(chatUseCase).recent(ME, "r1", 30L)
        verify(chatUseCase).before(ME, "r1", 100L, 30L)
        verify(chatUseCase).images(ME, "r1", 30L)
        verify(chatUseCase).history(ME, "r1")
        verify(chatUseCase).chats(ME, listOf(1L, 2L))
    }

    @Test
    fun 헤더가_없으면_유스케이스에_닿기_전에_403_이다() {
        mockMvc.perform(get("/api-public/chat/r1/page/30"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        mockMvc.perform(get("/api-public/chat/unread/7")).andExpect(status().isForbidden)

        verifyNoInteractions(chatUseCase, chatRoomUseCase)
    }

    @Test
    fun 미읽음은_경로의_id_와_헤더의_userId_를_함께_넘긴다() {
        whenever(chatRoomUseCase.unread(anyOrNull(), any())).thenReturn(listOf())

        mockMvc.perform(get("/api-public/chat/unread/7").header(HEADER, ME)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/chat/unread/$ME").header(HEADER, ME)).andExpect(status().isOk)

        verify(chatRoomUseCase).unread(ME, "7")
        verify(chatRoomUseCase).unread(ME, ME)
    }
}
