package com.example.memberservice.api.internal

import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * chat-service / ws-service 가 부르는 차단 조회 내부 API.
 * 정방향(blocked-ids)과 역방향(blocked-by)이 서로 다른 답을 주는지까지 본다 —
 * 차단은 단방향이라 이 둘이 뒤바뀌면 엉뚱한 사람의 메시지가 사라진다.
 */
class MemberInternalBlockTest : ApiTestSupport() {

    companion object {
        private const val TOKEN = "test-internal-token"
    }

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRwRepository

    private fun save(userId: String): Member = memberRepository.save(
        Member(userId = userId, email = "$userId@example.com", username = userId, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    /** 상태(차단·숨김)는 저장 전에 매긴다 — 테스트 트랜잭션이 없어 저장 뒤에 바꾼 값은 DB 로 가지 않는다. */
    private fun befriend(me: Member, friend: Member, setup: MemberFriend.() -> Unit = {}): MemberFriend =
        memberFriendRepository.save(MemberFriend.of(me, friend).apply(setup))

    @Test
    fun 차단한_사람만_blocked_ids_에_나온다() {
        val me = save("block-me")
        val blocked = save("block-b1")
        val hidden = save("block-h1")
        val normal = save("block-n1")

        befriend(me, blocked) { block() }
        befriend(me, hidden) { hide() }
        befriend(me, normal)

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", me.userId).header(InternalApiFilter.HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("block-b1"))
    }

    @Test
    fun 차단을_풀면_blocked_ids_에서_빠진다() {
        val me = save("unblock-me")
        val friend = save("unblock-f1")
        befriend(me, friend) {
            block()
            unblock()
        }

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", me.userId).header(InternalApiFilter.HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun blocked_by_는_나를_차단한_사람을_주고_내가_차단한_사람은_주지_않는다() {
        val target = save("by-target")
        val blocker = save("by-blocker")
        val blockedByTarget = save("by-victim")
        val notBlocking = save("by-friend")

        befriend(blocker, target) { block() } // blocker → target 차단 (역방향에 나와야 한다)
        befriend(target, blockedByTarget) { block() } // target 이 차단한 쪽 (역방향에 나오면 안 된다)
        befriend(notBlocking, target) // 그냥 친구

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-by", target.userId).header(InternalApiFilter.HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("by-blocker"))

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", target.userId).header(InternalApiFilter.HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("by-victim"))
    }

    @Test
    fun 없는_userId_의_blocked_by_는_빈_목록이다() {
        mockMvc.perform(get("/api-internal/member/{userId}/blocked-by", "no-such-user").header(InternalApiFilter.HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun 내부_토큰이_없으면_둘_다_거부한다() {
        mockMvc.perform(get("/api-internal/member/x/blocked-ids")).andExpect(status().isForbidden)
        mockMvc.perform(get("/api-internal/member/x/blocked-by")).andExpect(status().isForbidden)
        mockMvc.perform(get("/api-internal/member/x/blocked-ids").header(InternalApiFilter.HEADER, "wrong"))
            .andExpect(status().isForbidden)
    }
}
