package com.example.memberservice.api.internal

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.usage.MemberServiceUsageRepository
import com.example.memberservice.usage.ModuService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/** auth-service 가 부르는 이용 기록 API 와 백필용 bulk API. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberUsageInternalControllerTest {

    companion object {
        private const val TOKEN = "test-internal-token"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var usageRepository: MemberServiceUsageRepository

    private fun member(id: String): Member = memberRepository.save(
        Member(userId = "usageapi-$id", email = "usageapi-$id@example.com", username = id, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    private fun record(body: String) = mockMvc.perform(
        post("/api-internal/member/usage").header("X-Internal-Token", TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(body),
    )

    @Test
    fun 채팅_클라이언트로_기록하면_204_이고_CHAT_기록이_생긴다() {
        val m = member("chat")
        record("""{"userId":"${m.userId}","clientId":"modu-chat"}""").andExpect(status().isNoContent)
        record("""{"userId":"${m.userId}","clientId":"modu-commerce"}""").andExpect(status().isNoContent)

        assertThat(usageRepository.findAllByUserId(m.userId).map { it.service })
            .containsExactlyInAnyOrder(ModuService.CHAT, ModuService.COMMERCE)
    }

    @Test
    fun 모르는_클라이언트와_모르는_회원도_204_이고_기록하지_않는다() {
        val m = member("admin")
        record("""{"userId":"${m.userId}","clientId":"modu-admin"}""").andExpect(status().isNoContent)
        record("""{"userId":"usageapi-ghost","clientId":"modu-chat"}""").andExpect(status().isNoContent)

        assertThat(usageRepository.findAllByUserId(m.userId)).isEmpty()
        assertThat(usageRepository.findAllByUserId("usageapi-ghost")).isEmpty()
    }

    @Test
    fun bulk_는_넣은_줄_수를_돌려준다() {
        val a = member("bulk-a")
        val b = member("bulk-b")
        val body = """{"service":"COMMERCE","userIds":["${a.userId}","${b.userId}"],"usedAt":"2026-09-01T00:00:00"}"""

        mockMvc.perform(
            post("/api-internal/member/usage/bulk").header("X-Internal-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inserted").value(2))
        mockMvc.perform(
            post("/api-internal/member/usage/bulk").header("X-Internal-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inserted").value(0))
    }

    @Test
    fun bulk_가_천_개를_넘으면_400() {
        val ids = (0..1000).joinToString(",") { "\"x$it\"" }
        mockMvc.perform(
            post("/api-internal/member/usage/bulk").header("X-Internal-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content("""{"service":"CHAT","userIds":[$ids]}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun 내부_토큰이_없으면_거부한다() {
        mockMvc.perform(
            post("/api-internal/member/usage").contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"x","clientId":"modu-chat"}"""),
        )
            .andExpect(status().isForbidden)
        mockMvc.perform(
            post("/api-internal/member/usage/bulk").contentType(MediaType.APPLICATION_JSON)
                .content("""{"service":"CHAT","userIds":["x"]}"""),
        )
            .andExpect(status().isForbidden)
    }
}
