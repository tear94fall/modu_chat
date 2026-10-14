package com.example.memberservice.api.pub

import com.example.memberservice.api.client.chat.ChatFeignClient
import com.example.memberservice.api.client.commerce.CommerceClient
import com.example.memberservice.api.client.profile.ProfileFeignClient
import com.example.memberservice.api.client.push.PushFeignClient
import com.example.memberservice.api.client.storage.StorageFeignClient
import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 앱 API 의 본인 확인. 경로의 `{userId}` 는 "나"여야 한다 — 게이트웨이가 넣는 X-Auth-User-Id 와 다르면 403,
 * 헤더가 없으면(게이트웨이를 거치지 않은 호출) 403. 남을 찾는 조회(이메일·회원 id·친구 찾기)는 남의 것도 된다.
 * 실제 DB 까지 가서, 막힌 요청이 아무것도 바꾸지 않았는지도 본다.
 */
class MemberPublicIdentityTest : ApiTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRwRepository
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient
    @MockitoBean lateinit var chatFeignClient: ChatFeignClient
    @MockitoBean lateinit var pushFeignClient: PushFeignClient
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient
    @MockitoBean lateinit var commerceClient: CommerceClient

    private lateinit var me: Member
    private lateinit var victim: Member
    private lateinit var friend: Member

    private fun member(tag: String) = memberRepository.save(
        Member(
            userId = "id-$tag", email = "$tag@identity.test", username = tag, statusMessage = "상태-$tag",
            profiles = mutableListOf(), chatRoomMembers = mutableListOf(),
        ),
    )

    @BeforeEach
    fun setUp() {
        me = member("me")
        victim = member("victim")
        friend = member("friend")
        memberFriendRepository.save(MemberFriend.of(me, friend))
        memberFriendRepository.save(MemberFriend.of(victim, friend))
        whenever(profileFeignClient.getMemberProfiles(anyLong())).thenReturn(ResponseEntity.ok(listOf()))
    }

    private class Call(val name: String, val method: HttpMethod, val path: (String) -> String, val body: String? = null)

    /** 경로의 {userId} 가 "나"인 API 전부. path 는 userId 를 받아 경로를 만든다. */
    private fun selfCalls(): List<Call> {
        val friendId = friend.id
        return listOf(
            Call("내 프로필 수정", HttpMethod.POST, { "/api-public/member/$it" }, """{"username":"바뀐이름","statusMessage":"","profileImage":"","wallpaperImage":""}"""),
            Call("친구 목록", HttpMethod.GET, { "/api-public/member/$it/friends" }),
            Call("차단한 친구 userId 목록", HttpMethod.GET, { "/api-public/member/$it/friends/blocked-ids" }),
            Call("친구 이름표", HttpMethod.GET, { "/api-public/member/$it/friends/names" }),
            Call("친구 추가", HttpMethod.POST, { "/api-public/member/$it/friends" }, """{"email":"friend@identity.test"}"""),
            Call("친구 한 명", HttpMethod.GET, { "/api-public/member/$it/friends/$friendId" }),
            Call("친구 이름 변경", HttpMethod.PUT, { "/api-public/member/$it/friends/$friendId/name" }, """{"name":"바뀐별칭"}"""),
            Call("친구 즐겨찾기", HttpMethod.PUT, { "/api-public/member/$it/friends/$friendId/favorite" }, """{"on":true}"""),
            Call("친구 숨기기", HttpMethod.PUT, { "/api-public/member/$it/friends/$friendId/hidden" }, """{"on":true}"""),
            Call("친구 차단", HttpMethod.PUT, { "/api-public/member/$it/friends/$friendId/blocked" }, """{"on":true}"""),
            Call("회원 탈퇴", HttpMethod.DELETE, { "/api-public/member/$it" }),
        )
    }

    private fun perform(call: Call, pathUserId: String, authUserId: String?): ResultActions {
        val req = request(call.method, call.path(pathUserId))
        authUserId?.let { req.header(AUTH_USER_ID_HEADER, it) }
        call.body?.let { req.contentType(MediaType.APPLICATION_JSON).content(it) }
        return mockMvc.perform(req)
    }

    /** JUnit 은 @TestFactory 메서드 하나에 @BeforeEach 를 한 번만 돌린다 — 막힌 요청은 아무것도 바꾸지 않으므로 같은 데이터를 계속 쓴다. */
    @TestFactory
    fun 남의_userId_면_403이고_아무것도_바뀌지_않는다(): List<DynamicTest> = selfCalls().map { call ->
        DynamicTest.dynamicTest(call.name) {
            perform(call, victim.userId, me.userId)
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            assertVictimUntouched()
        }
    }

    @TestFactory
    fun 헤더가_없으면_403이고_아무것도_바뀌지_않는다(): List<DynamicTest> = selfCalls().map { call ->
        DynamicTest.dynamicTest(call.name) {
            perform(call, victim.userId, null).andExpect(status().isForbidden)
            assertVictimUntouched()
        }
    }

    /** 빈 헤더도 없는 것과 같다. */
    @Test
    fun 헤더가_비어_있어도_403() {
        mockMvc.perform(
            request(HttpMethod.GET, "/api-public/member/${me.userId}/friends").header(AUTH_USER_ID_HEADER, " "),
        ).andExpect(status().isForbidden)
    }

    /** 탈퇴는 마지막에 둔다(selfCalls 의 순서) — 앞의 요청들이 "나"로 통과하는지 본 뒤에 탈퇴한다. */
    @TestFactory
    fun 내_userId_면_통과한다(): List<DynamicTest> = selfCalls().map { call ->
        DynamicTest.dynamicTest(call.name) {
            perform(call, me.userId, me.userId).andExpect(status().is2xxSuccessful)
        }
    }

    @Test
    fun 남을_찾는_조회는_남의_것도_된다() {
        val auth = me.userId
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/${victim.email}").header(AUTH_USER_ID_HEADER, auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(victim.userId))
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/member/${victim.id}").header(AUTH_USER_ID_HEADER, auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(victim.userId))
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/friends/${victim.email}").header(AUTH_USER_ID_HEADER, auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].userId").value(victim.userId))
        mockMvc.perform(request(HttpMethod.GET, "/api-public/notice").header(AUTH_USER_ID_HEADER, auth)).andExpect(status().isOk)
    }

    /** 남을 찾는 조회도 게이트웨이를 거쳐야 한다(헤더가 없으면 403). */
    @Test
    fun 남을_찾는_조회도_헤더가_없으면_403() {
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/${victim.email}")).andExpect(status().isForbidden)
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/member/${victim.id}")).andExpect(status().isForbidden)
        mockMvc.perform(request(HttpMethod.GET, "/api-public/member/friends/${victim.email}")).andExpect(status().isForbidden)
        mockMvc.perform(request(HttpMethod.GET, "/api-public/notice")).andExpect(status().isForbidden)
        mockMvc.perform(request(HttpMethod.GET, "/api-public/common/version")).andExpect(status().isForbidden)
    }

    private fun assertVictimUntouched() {
        val after = memberRepository.findById(victim.id!!).orElseThrow()
        assertThat(after.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(after.username).isEqualTo("victim")
        assertThat(after.statusMessage).isEqualTo("상태-victim")
        val row = memberFriendRepository.findByMemberIdAndFriendId(victim.id!!, friend.id!!).orElseThrow()
        assertThat(row.friendName).isEqualTo("friend")
        assertThat(row.favorite).isFalse()
        assertThat(row.status.name).isEqualTo("NORMAL")
        assertThat(memberFriendRepository.countByMemberId(victim.id!!)).isEqualTo(1)
    }
}
