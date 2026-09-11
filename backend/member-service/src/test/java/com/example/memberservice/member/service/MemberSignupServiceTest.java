package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.memberservice.member.dto.GoogleAccountDto;
import com.example.memberservice.member.dto.MemberDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 트랜잭션 밖에서의 재시도 로직만 검증한다. Spring 컨텍스트 없이 순수 Mockito 로 MemberService 를
 * 목킹해서, findOrCreateGoogleMember 가 던지는 DataIntegrityViolationException 을 findOrCreate 가 정확히 한 번만
 * 다시 시도한다는 것과, 재시도까지 실패하면 그대로 전파한다는 것을 확인한다.
 */
class MemberSignupServiceTest {

    private MemberService memberService;
    private MemberSignupService memberSignupService;
    private GoogleAccountDto request;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        memberSignupService = new MemberSignupService(memberService);

        request = new GoogleAccountDto("sub-1", "dup@example.com", "이름", "");
    }

    @Test
    void firstAttemptHitsUniqueConstraint_retriesOnceAndReturnsSecondResult() {
        MemberDto expected = MemberDto.builder().id(1L).email("dup@example.com").build();

        given(memberService.findOrCreateGoogleMember(request))
                .willThrow(new DataIntegrityViolationException("dup"))
                .willReturn(expected);

        MemberDto actual = memberSignupService.findOrCreate(request);

        assertThat(actual).isEqualTo(expected);
        verify(memberService, times(2)).findOrCreateGoogleMember(request);
    }

    @Test
    void secondAttemptAlsoFails_propagatesAndDoesNotRetryAgain() {
        given(memberService.findOrCreateGoogleMember(request))
                .willThrow(new DataIntegrityViolationException("dup-1"))
                .willThrow(new DataIntegrityViolationException("dup-2"));

        assertThatThrownBy(() -> memberSignupService.findOrCreate(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("dup-2");

        verify(memberService, times(2)).findOrCreateGoogleMember(request);
    }

    @Test
    void happyPath_callsCreateMemberExactlyOnce() {
        MemberDto expected = MemberDto.builder().id(2L).email("new@example.com").build();
        given(memberService.findOrCreateGoogleMember(request)).willReturn(expected);

        MemberDto actual = memberSignupService.findOrCreate(request);

        assertThat(actual).isEqualTo(expected);
        verify(memberService, times(1)).findOrCreateGoogleMember(request);
    }
}
