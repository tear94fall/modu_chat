package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.port.ProfileInfo
import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import com.example.memberservice.application.support.ApplicationTestSupport
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

/**
 * 재설치·기기 변경 시 이미 가입된 이메일이면 예외 대신 기존 회원을 그대로 돌려줘야 한다는
 * 멱등성 요구사항을 검증한다. 구글 검증은 auth-service 가 하므로 여기서는 검증된 계정을 바로 넣는다.
 * storage·profile 호출(포트)은 목이다.
 */
class MemberSignupIdempotencyTest : ApplicationTestSupport() {

    companion object {
        private const val UPLOADED_FILE = "uploaded-profile.png"
    }

    @Autowired lateinit var memberSignupUseCase: MemberSignupUseCase
    @Autowired lateinit var memberRepository: MemberRwRepository

    private fun account(email: String) =
        GoogleAccountCommand("google-sub-" + UUID.randomUUID(), email, "테스트유저", "https://example.com/picture.jpg")

    @Test
    fun signingUpTwice_withSameEmail_createsOnlyOneMember() {
        val email = "returning-" + UUID.randomUUID() + "@example.com"
        val account = account(email)
        stubProfileCollaborators()

        val before = memberRepository.count()

        val first = memberSignupUseCase.findOrCreate(account)
        val second = memberSignupUseCase.findOrCreate(account)

        val after = memberRepository.count()

        assertThat(after - before).isEqualTo(1)
        assertThat(second.id).isEqualTo(first.id)
        assertThat(second.email).isEqualTo(email)
    }

    @Test
    fun secondSignup_doesNotRepeatProfileImageCreation() {
        val account = account("returning-" + UUID.randomUUID() + "@example.com")
        stubProfileCollaborators()

        memberSignupUseCase.findOrCreate(account)
        clearInvocations(storagePort, profilePort)

        memberSignupUseCase.findOrCreate(account)

        verify(storagePort, never()).uploadFromUrl(any())
        verify(profilePort, never()).addProfile(any())
    }

    /** 첫 가입은 구글 사진 주소를 storage 에 올리고, 올라간 파일 이름이 회원의 프로필 사진이 된다. */
    @Test
    fun firstSignup_uploadsGooglePicture_andAppliesItToTheMember() {
        val account = account("pictured-" + UUID.randomUUID() + "@example.com")
        stubProfileCollaborators()

        val created = memberSignupUseCase.findOrCreate(account)

        verify(storagePort).uploadFromUrl(eq("https://example.com/picture.jpg"))
        assertThat(created.profileImage).isEqualTo(UPLOADED_FILE)
        assertThat(memberRepository.findById(created.id!!).orElseThrow().profileImage).isEqualTo(UPLOADED_FILE)
    }

    /** 사진 단계가 실패하면 가입도 없던 일이 된다(예전에는 한 트랜잭션이라 함께 롤백됐다). 다음 로그인 때 처음부터 다시 한다. */
    @Test
    fun pictureStepFails_signupIsUndone_andNextLoginStartsOver() {
        val email = "unlucky-" + UUID.randomUUID() + "@example.com"
        val account = account(email)
        whenever(storagePort.uploadFromUrl(any())).thenThrow(RuntimeException("storage down"))

        assertThatThrownBy { memberSignupUseCase.findOrCreate(account) }.hasMessage("storage down")
        assertThat(memberRepository.existsByEmail(email)).isFalse()

        org.mockito.Mockito.reset(storagePort)
        stubProfileCollaborators()
        val retried = memberSignupUseCase.findOrCreate(account)

        assertThat(retried.email).isEqualTo(email)
        assertThat(retried.profileImage).isEqualTo(UPLOADED_FILE)
    }

    @Test
    fun unknownEmail_createsNewMember() {
        val email = "brand-new-" + UUID.randomUUID() + "@example.com"
        stubProfileCollaborators()

        val response = memberSignupUseCase.findOrCreate(account(email))

        assertThat(response.id).isNotNull()
        assertThat(response.email).isEqualTo(email)
        assertThat(memberRepository.existsByEmail(email)).isTrue()
    }

    private fun stubProfileCollaborators() {
        whenever(storagePort.uploadFromUrl(any())).thenReturn(UPLOADED_FILE)
        whenever(profilePort.addProfile(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<ProfileInfo>(0)
            ProfileInfo(1L, request.memberId, request.profileType, UPLOADED_FILE, "", "")
        }
        whenever(profilePort.getMemberProfiles(anyLong())).thenReturn(listOf())
    }
}
