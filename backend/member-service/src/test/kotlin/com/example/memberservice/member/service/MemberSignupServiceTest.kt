package com.example.memberservice.member.service

import com.example.memberservice.member.dto.GoogleAccountDto
import com.example.memberservice.member.dto.MemberDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

/**
 * 트랜잭션 밖에서의 재시도 로직만 검증한다. Spring 컨텍스트 없이 순수 Mockito 로 MemberService 를
 * 목킹해서, findOrCreateGoogleMember 가 던지는 DataIntegrityViolationException 을 findOrCreate 가 정확히 한 번만
 * 다시 시도한다는 것과, 재시도까지 실패하면 그대로 전파한다는 것을 확인한다.
 */
class MemberSignupServiceTest {

    private lateinit var memberService: MemberService
    private lateinit var memberSignupService: MemberSignupService
    private lateinit var request: GoogleAccountDto

    @BeforeEach
    fun setUp() {
        memberService = mock()
        memberSignupService = MemberSignupService(memberService)

        request = GoogleAccountDto("sub-1", "dup@example.com", "이름", "")
    }

    @Test
    fun firstAttemptHitsUniqueConstraint_retriesOnceAndReturnsSecondResult() {
        val expected = MemberDto(id = 1L, email = "dup@example.com")

        whenever(memberService.findOrCreateGoogleMember(request))
            .thenThrow(DataIntegrityViolationException("dup"))
            .thenReturn(expected)

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
        val expected = MemberDto(id = 2L, email = "new@example.com")
        whenever(memberService.findOrCreateGoogleMember(request)).thenReturn(expected)

        val actual = memberSignupService.findOrCreate(request)

        assertThat(actual).isEqualTo(expected)
        verify(memberService, times(1)).findOrCreateGoogleMember(request)
    }
}
