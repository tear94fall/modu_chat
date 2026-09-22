package com.example.memberservice.api.pub

import com.example.memberservice.member.service.MemberService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 탈퇴는 본인만. 게이트웨이가 넣는 X-Auth-User-Id 와 경로의 userId 가 같아야 한다. */
@SpringBootTest
@AutoConfigureMockMvc
class MemberPublicControllerWithdrawTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberService: MemberService

    @Test
    fun withdraw_self_is204() {
        mockMvc.perform(delete("/api-public/member/me-123").header("X-Auth-User-Id", "me-123"))
            .andExpect(status().isNoContent)
        verify(memberService).withdraw("me-123")
    }

    @Test
    fun withdraw_someoneElse_is403() {
        mockMvc.perform(delete("/api-public/member/victim").header("X-Auth-User-Id", "me-123"))
            .andExpect(status().isForbidden)
        verify(memberService, never()).withdraw("victim")
    }

    @Test
    fun withdraw_withoutAuthHeader_is403() {
        mockMvc.perform(delete("/api-public/member/me-123"))
            .andExpect(status().isForbidden)
        verify(memberService, never()).withdraw("me-123")
    }
}
