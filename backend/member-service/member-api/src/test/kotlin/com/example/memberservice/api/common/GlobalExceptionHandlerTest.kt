package com.example.memberservice.api.common

import com.example.memberservice.api.client.profile.ProfileFeignClient
import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 예전에는 전역 예외 처리기가 없어 CustomException 이 모두 500 이었다. 이제 ErrorCode 의 상태와 `{message, code}` 로 답한다. */
class GlobalExceptionHandlerTest : ApiTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient

    private fun internal(path: String) = get(path).header(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN)

    @Test
    fun 없는_회원은_404_와_코드로_답한다() {
        mockMvc.perform(internal("/api-internal/member/id/nobody"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USERID_NOT_FOUND_ERROR"))
            .andExpect(jsonPath("$.message").exists())
        mockMvc.perform(internal("/api-internal/member/by-email/nobody@example.com"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"))
        mockMvc.perform(internal("/api-internal/member/nobody/role")).andExpect(status().isNotFound)
        mockMvc.perform(internal("/api-internal/member/nobody/blocked-ids")).andExpect(status().isNotFound)
        mockMvc.perform(internal("/api-admin/member/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MEMBER_ID_NOT_FOUND_ERROR"))
        mockMvc.perform(internal("/api-admin/member/999999/friends")).andExpect(status().isNotFound)
        mockMvc.perform(get("/api-public/member/nobody@example.com").header(AUTH_USER_ID_HEADER, "u1"))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/api-public/member/member/999999").header(AUTH_USER_ID_HEADER, "u1"))
            .andExpect(status().isNotFound)
    }

    /** 나를 차단한 사람 목록은 회원을 먼저 찾지 않는다 — 없는 userId 여도 빈 목록이다. */
    @Test
    fun 없는_회원의_blockedBy_는_빈_목록이다() {
        mockMvc.perform(internal("/api-internal/member/nobody/blocked-by"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun 친구_추가에서_없는_이메일은_404_친구가_아니면_404_다() {
        val me = memberRepository.save(Member(userId = "eh-me", email = "eh-me@example.com", username = "나", profiles = mutableListOf(), chatRoomMembers = mutableListOf()))

        mockMvc.perform(
            post("/api-public/member/${me.userId}/friends").header(AUTH_USER_ID_HEADER, me.userId)
                .contentType(MediaType.APPLICATION_JSON).content("""{"email":"ghost@example.com"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"))
        mockMvc.perform(
            put("/api-public/member/${me.userId}/friends/424242/favorite").header(AUTH_USER_ID_HEADER, me.userId)
                .contentType(MediaType.APPLICATION_JSON).content("""{"on":true}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("FRIEND_NOT_FOUND_ERROR"))
    }

    @Test
    fun 잘못된_요청은_400_없는_경로는_404_메서드가_틀리면_405_다() {
        mockMvc.perform(internal("/api-admin/member").param("sort", "nope"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(internal("/api-admin/member/not-a-number")).andExpect(status().isBadRequest)
        mockMvc.perform(
            post("/api-internal/member/google").header(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content("{not json"),
        ).andExpect(status().isBadRequest)
        mockMvc.perform(internal("/api-internal/no-such-path")).andExpect(status().isNotFound)
        mockMvc.perform(put("/api-internal/member/google").header(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN))
            .andExpect(status().isMethodNotAllowed)
    }
}
