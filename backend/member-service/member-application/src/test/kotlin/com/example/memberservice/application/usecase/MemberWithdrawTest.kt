package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.support.ApplicationTestSupport
import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.entity.MemberStatus
import java.util.UUID
import com.example.memberservice.application.port.ProfileInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 탈퇴: 다른 서비스 정리를 요청하고, 친구 관계를 지우고, 회원 행은 개인정보만 비운 채 남긴다.
 * 다른 서비스 호출(포트)은 목이다.
 */
class MemberWithdrawTest : ApplicationTestSupport() {

    @Autowired lateinit var memberWithdrawUseCase: MemberWithdrawUseCase
    @Autowired lateinit var memberSignupUseCase: MemberSignupUseCase
    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRwRepository

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

        memberWithdrawUseCase.withdraw(me.userId)

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

        // 순서는 예전과 같다: 방 나가기 → 푸시 토큰 → 커머스 → 사진 파일.
        val order = inOrder(chatRoomPort, pushPort, commercePort, storagePort)
        order.verify(chatRoomPort).exitAllChatRooms(me.id!!)
        order.verify(pushPort).deleteToken(me.userId)
        order.verify(commercePort).deleteCustomer(me.userId)
        order.verify(storagePort).delete("profile-$tag.png")
        order.verify(storagePort).delete("wall-$tag.jpg")
    }

    /** 방 나가기가 실패하면 탈퇴도 되지 않는다 — 뒤의 정리도 하지 않고, 회원과 친구 관계가 그대로 남는다. 앱이 다시 시도한다. */
    @Test
    fun withdraw_fails_whenChatCleanupFails_andNothingIsChanged() {
        val tag = tag()
        val me = saveMember(tag)
        val friend = saveMember("$tag-f")
        memberFriendRepository.save(MemberFriend.of(me, friend))
        doThrow(RuntimeException("chat down")).whenever(chatRoomPort).exitAllChatRooms(anyLong())

        assertThatThrownBy { memberWithdrawUseCase.withdraw(me.userId) }.hasMessage("chat down")

        verify(pushPort, never()).deleteToken(any())
        verify(storagePort, never()).delete(any())
        assertThat(memberRepository.findById(me.id!!).orElseThrow().status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(me.id!!, friend.id!!)).isPresent
    }

    /** 사진 파일 삭제가 실패해도 탈퇴는 된다. */
    @Test
    fun withdraw_stillSucceeds_whenFileDeleteFails() {
        val me = saveMember(tag())
        doThrow(RuntimeException("storage down")).whenever(storagePort).delete(any())

        memberWithdrawUseCase.withdraw(me.userId)

        assertThat(memberRepository.findById(me.id!!).orElseThrow().status).isEqualTo(MemberStatus.WITHDRAWN)
    }

    @Test
    fun withdraw_stillSucceeds_whenCommerceCallFails() {
        val me = saveMember(tag())
        doThrow(RuntimeException("I/O error on DELETE: Read timed out")).whenever(commercePort).deleteCustomer(any())

        memberWithdrawUseCase.withdraw(me.userId)

        verify(commercePort).deleteCustomer(me.userId)
        val after = memberRepository.findById(me.id!!).orElseThrow()
        assertThat(after.status).isEqualTo(MemberStatus.WITHDRAWN)
        assertThat(after.username).isEqualTo(Member.WITHDRAWN_USERNAME)
    }

    @Test
    fun withdraw_twice_isIdempotent() {
        val me = saveMember(tag())

        memberWithdrawUseCase.withdraw(me.userId)
        memberWithdrawUseCase.withdraw(me.userId)

        verify(chatRoomPort).exitAllChatRooms(me.id!!) // 두 번째는 아무것도 안 한다
    }

    @Test
    fun googleSignIn_afterWithdrawal_reactivatesTheSameRow() {
        val tag = tag()
        val me = saveMember(tag)
        memberWithdrawUseCase.withdraw(me.userId)

        // 같은 구글 계정(sub)으로 다시 로그인. 사진은 없다고 두어 storage 업로드 경로를 타지 않는다.
        val account = GoogleAccountCommand(me.userId, "$tag@example.com", "돌아온 이름", null)
        val again = memberSignupUseCase.findOrCreate(account)

        assertThat(again.id).isEqualTo(me.id)
        val after = memberRepository.findById(me.id!!).orElseThrow()
        assertThat(after.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(after.email).isEqualTo("$tag@example.com")
        assertThat(after.username).isEqualTo("돌아온 이름")
        assertThat(after.withdrawnDate).isNull()
        verify(storagePort, never()).uploadFromUrl(any())
    }

    /** 되살린 회원에게 구글 사진이 있으면 그 사진 주소를 올린다(예전에는 비워진 프로필 사진 값 "" 을 올리려 했다). */
    @Test
    fun googleSignIn_afterWithdrawal_uploadsTheGooglePicture() {
        val tag = tag()
        val me = saveMember(tag)
        memberWithdrawUseCase.withdraw(me.userId)
        whenever(storagePort.uploadFromUrl(any())).thenReturn("revived.png")
        whenever(profilePort.addProfile(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<ProfileInfo>(0)
            ProfileInfo(7L, request.memberId, request.profileType, "revived.png", "", "")
        }

        val again = memberSignupUseCase.findOrCreate(
            GoogleAccountCommand(me.userId, "$tag@example.com", "돌아온 이름", "https://example.com/new.jpg"),
        )

        verify(storagePort).uploadFromUrl("https://example.com/new.jpg")
        assertThat(again.id).isEqualTo(me.id)
        assertThat(again.profileImage).isEqualTo("revived.png")
        assertThat(memberRepository.findById(me.id!!).orElseThrow().status).isEqualTo(MemberStatus.ACTIVE)
    }
}
