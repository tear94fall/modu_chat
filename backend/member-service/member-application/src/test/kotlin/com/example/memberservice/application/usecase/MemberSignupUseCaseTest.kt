package com.example.memberservice.application.usecase

import com.example.memberservice.application.port.ProfilePort
import com.example.memberservice.application.port.StoragePort
import com.example.memberservice.application.service.MemberCommandService
import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import com.example.memberservice.application.usecase.result.GoogleMemberResult
import com.example.memberservice.application.usecase.result.MemberResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

/**
 * 트랜잭션 밖에서의 재시도 로직만 검증한다. Spring 컨텍스트 없이 순수 Mockito 로 MemberCommandService 를
 * 목킹해서, findOrCreateGoogleMember 가 던지는 DataIntegrityViolationException 을 findOrCreate 가 정확히 한 번만
 * 다시 시도한다는 것과, 재시도까지 실패하면 그대로 전파한다는 것을 확인한다.
 */
class MemberSignupUseCaseTest {

    private lateinit var memberService: MemberCommandService
    private lateinit var storagePort: StoragePort
    private lateinit var memberSignupService: MemberSignupUseCase
    private lateinit var request: GoogleAccountCommand

    @BeforeEach
    fun setUp() {
        memberService = mock()
        storagePort = mock()
        memberSignupService = MemberSignupUseCase(memberService, storagePort, mock<ProfilePort>())

        request = GoogleAccountCommand("sub-1", "dup@example.com", "이름", "")
    }

    @Test
    fun firstAttemptHitsUniqueConstraint_retriesOnceAndReturnsSecondResult() {
        val expected = member(1L, "dup@example.com")

        whenever(memberService.findOrCreateGoogleMember(request))
            .thenThrow(DataIntegrityViolationException("dup"))
            .thenReturn(GoogleMemberResult(expected, fresh = false))

        val actual = memberSignupService.findOrCreate(request)

        assertThat(actual).isEqualTo(expected)
        verify(memberService, times(2)).findOrCreateGoogleMember(request)
    }

    @Test
    fun secondAttemptAlsoFails_propagatesAndDoesNotRetryAgain() {
        whenever(memberService.findOrCreateGoogleMember(request))
            .thenThrow(DataIntegrityViolationException("dup-1"))
            .thenThrow(DataIntegrityViolationException("dup-2"))

        assertThatThrownBy { memberSignupService.findOrCreate(request) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessage("dup-2")

        verify(memberService, times(2)).findOrCreateGoogleMember(request)
    }

    @Test
    fun happyPath_callsCreateMemberExactlyOnce() {
        val expected = member(2L, "new@example.com")
        whenever(memberService.findOrCreateGoogleMember(request)).thenReturn(GoogleMemberResult(expected, fresh = false))

        val actual = memberSignupService.findOrCreate(request)

        assertThat(actual).isEqualTo(expected)
        verify(memberService, times(1)).findOrCreateGoogleMember(request)
        // 구글 사진이 없는 계정이라 새 회원이어도 storage 를 부르지 않는다.
        verifyNoInteractions(storagePort)
    }

    private fun member(id: Long, email: String) = MemberResult(id, "sub-1", "google", null, email, "이름", "", "", "")
}
