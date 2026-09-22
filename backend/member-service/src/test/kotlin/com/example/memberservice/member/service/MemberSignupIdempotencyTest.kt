package com.example.memberservice.member.service

import com.example.memberservice.member.dto.GoogleAccountDto
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.profile.client.ProfileFeignClient
import com.example.memberservice.profile.dto.ProfileDto
import com.example.memberservice.storage.client.StorageFeignClient
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
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

/**
 * 재설치·기기 변경 시 이미 가입된 이메일이면 예외 대신 기존 회원을 그대로 돌려줘야 한다는
 * 멱등성 요구사항을 검증한다. 구글 검증은 auth-service 가 하므로 여기서는 검증된 계정 DTO 를 바로 넣는다.
 */
@SpringBootTest
@Transactional
class MemberSignupIdempotencyTest {

    companion object {
        private const val UPLOADED_FILE = "uploaded-profile.png"
    }

    @Autowired lateinit var memberService: MemberService
    @Autowired lateinit var memberRepository: MemberRepository
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient

    private fun account(email: String) =
        GoogleAccountDto("google-sub-" + UUID.randomUUID(), email, "테스트유저", "https://example.com/picture.jpg")

    @Test
    fun signingUpTwice_withSameEmail_createsOnlyOneMember() {
        val email = "returning-" + UUID.randomUUID() + "@example.com"
        val account = account(email)
        stubProfileCollaborators()

        val before = memberRepository.count()

        val first = memberService.findOrCreateGoogleMember(account)
        val second = memberService.findOrCreateGoogleMember(account)

        val after = memberRepository.count()

        assertThat(after - before).isEqualTo(1)
        assertThat(second.id).isEqualTo(first.id)
        assertThat(second.email).isEqualTo(email)
    }

    @Test
    fun secondSignup_doesNotRepeatProfileImageCreation() {
        val account = account("returning-" + UUID.randomUUID() + "@example.com")
        stubProfileCollaborators()

        memberService.findOrCreateGoogleMember(account)
        clearInvocations(storageFeignClient, profileFeignClient)

        memberService.findOrCreateGoogleMember(account)

        verify(storageFeignClient, never()).upload(any<String>())
        verify(profileFeignClient, never()).addProfileRequest(any())
    }

    @Test
    fun unknownEmail_createsNewMember() {
        val email = "brand-new-" + UUID.randomUUID() + "@example.com"
        stubProfileCollaborators()

        val response = memberService.findOrCreateGoogleMember(account(email))

        assertThat(response.id).isNotNull()
        assertThat(response.email).isEqualTo(email)
        assertThat(memberRepository.existsByEmail(email)).isTrue()
    }

    private fun stubProfileCollaborators() {
        whenever(storageFeignClient.upload(any<String>())).thenReturn(ResponseEntity.ok(UPLOADED_FILE))
        whenever(profileFeignClient.addProfileRequest(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<ProfileDto>(0)
            ResponseEntity.ok(ProfileDto.from(1L, request.memberId, request.profileType, UPLOADED_FILE, "", ""))
        }
        whenever(profileFeignClient.getMemberProfiles(anyLong())).thenReturn(ResponseEntity.ok(listOf()))
    }
}
