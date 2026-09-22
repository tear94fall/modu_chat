package com.example.memberservice.member.service

import com.example.memberservice.chat.client.ChatFeignClient
import com.example.memberservice.member.dto.GoogleAccountDto
import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.entity.MemberStatus
import com.example.memberservice.member.repository.MemberFriendRepository
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.notice.client.PushFeignClient
import com.example.memberservice.profile.client.ProfileFeignClient
import com.example.memberservice.storage.client.StorageFeignClient
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

/** 회원 탈퇴: 다른 서비스 정리를 요청하고, 친구 관계를 지우고, 회원 행은 개인정보만 비운 채 남긴다. */
@SpringBootTest
class MemberWithdrawTest {

    @Autowired lateinit var memberService: MemberService
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRepository

    @MockitoBean lateinit var chatFeignClient: ChatFeignClient
    @MockitoBean lateinit var pushFeignClient: PushFeignClient
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient

    private fun saveMember(tag: String): Member = memberRepository.save(
        Member(
            userId = "sub-$tag",
            auth = "google",
            email = "$tag@example.com",
            username = "이름-$tag",
            statusMessage = "상태",
            profileImage = "profile-$tag.png",
            wallpaperImage = "wall-$tag.jpg",
            profiles = mutableListOf(),
            chatRoomMembers = mutableListOf(1L, 2L),
        ),
    )

    private fun tag() = UUID.randomUUID().toString().substring(0, 8)

    // 지연 로딩 컬렉션(chatRoomMembers)을 검증하려면 같은 세션 안이어야 한다.
    @Test
    @Transactional
    fun withdraw_scrubsPersonalData_dropsFriendsBothWays_andCleansOtherServices() {
        val tag = tag()
        val me = saveMember(tag)
        val friend = saveMember("$tag-f")
        memberFriendRepository.save(MemberFriend.of(me, friend))
        memberFriendRepository.save(MemberFriend.of(friend, me))
        whenever(chatFeignClient.exitAllChatRooms(me.id!!)).thenReturn(listOf(1L, 2L))
        whenever(pushFeignClient.deleteToken(any())).thenReturn(ResponseEntity.noContent().build())
        whenever(storageFeignClient.delete(any())).thenReturn(ResponseEntity.ok("ok"))

        memberService.withdraw(me.userId)

        val after = memberRepository.findById(me.id!!).orElseThrow()
        assertThat(after.status).isEqualTo(MemberStatus.WITHDRAWN)
        assertThat(after.withdrawnDate).isNotNull()
        assertThat(after.userId).isEqualTo(me.userId) // 채팅 기록과의 연결은 남긴다
        assertThat(after.email).doesNotContain("@example.com") // 같은 구글 계정으로 다시 가입할 수 있어야 한다
        assertThat(after.username).isEqualTo(Member.WITHDRAWN_USERNAME)
        assertThat(after.statusMessage).isEmpty()
        assertThat(after.profileImage).isEmpty()
        assertThat(after.wallpaperImage).isEmpty()
        assertThat(after.chatRoomMembers).isEmpty()

        assertThat(memberFriendRepository.findByMemberIdAndFriendId(me.id!!, friend.id!!)).isEmpty
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(friend.id!!, me.id!!)).isEmpty

        verify(chatFeignClient).exitAllChatRooms(me.id!!)
        verify(pushFeignClient).deleteToken(me.userId)
        verify(storageFeignClient).delete("profile-$tag.png")
        verify(storageFeignClient).delete("wall-$tag.jpg")
    }

    @Test
    fun withdraw_twice_isIdempotent() {
        val me = saveMember(tag())
        whenever(chatFeignClient.exitAllChatRooms(anyLong())).thenReturn(listOf())
        whenever(pushFeignClient.deleteToken(any())).thenReturn(ResponseEntity.noContent().build())

        memberService.withdraw(me.userId)
        memberService.withdraw(me.userId)

        verify(chatFeignClient).exitAllChatRooms(me.id!!) // 두 번째는 아무것도 안 한다
    }

    @Test
    fun googleSignIn_afterWithdrawal_reactivatesTheSameRow() {
        val tag = tag()
        val me = saveMember(tag)
        whenever(chatFeignClient.exitAllChatRooms(anyLong())).thenReturn(listOf())
        whenever(pushFeignClient.deleteToken(any())).thenReturn(ResponseEntity.noContent().build())
        whenever(profileFeignClient.getMemberProfiles(anyLong())).thenReturn(ResponseEntity.ok(listOf()))
        memberService.withdraw(me.userId)

        // 같은 구글 계정(sub)으로 다시 로그인. 사진은 없다고 두어 storage 업로드 경로를 타지 않는다.
        val account = GoogleAccountDto(me.userId, "$tag@example.com", "돌아온 이름", null)
        val again = memberService.findOrCreateGoogleMember(account)

        assertThat(again.id).isEqualTo(me.id)
        val after = memberRepository.findById(me.id!!).orElseThrow()
        assertThat(after.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(after.email).isEqualTo("$tag@example.com")
        assertThat(after.username).isEqualTo("돌아온 이름")
        assertThat(after.withdrawnDate).isNull()
        verify(storageFeignClient, never()).upload(any<String>())
    }
}
