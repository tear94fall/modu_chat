package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.example.memberservice.member.dto.ResponseMemberDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 트랜잭션 밖에서의 재시도 로직만 검증한다. Spring 컨텍스트 없이 순수 Mockito 로 MemberService 를
 * 목킹해서, createMember 가 던지는 DataIntegrityViolationException 을 signup 이 정확히 한 번만
 * 다시 시도한다는 것과, 재시도까지 실패하면 그대로 전파한다는 것을 확인한다.
 */
class MemberSignupServiceTest {

    private MemberService memberService;
    private MemberSignupService memberSignupService;
    private GoogleLoginRequest request;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        memberSignupService = new MemberSignupService(memberService);

        request = new GoogleLoginRequest();
        request.setAuthType("google");
        request.setIdToken("token");
    }

    @Test
    void firstAttemptHitsUniqueConstraint_retriesOnceAndReturnsSecondResult() {
        ResponseMemberDto expected = ResponseMemberDto.builder().id(1L).email("dup@example.com").build();

        given(memberService.createMember(request))
                .willThrow(new DataIntegrityViolationException("dup"))
                .willReturn(expected);

        ResponseMemberDto actual = memberSignupService.signup(request);

        assertThat(actual).isEqualTo(expected);
        verify(memberService, times(2)).createMember(request);
    }

    @Test
    void secondAttemptAlsoFails_propagatesAndDoesNotRetryAgain() {
        given(memberService.createMember(request))
                .willThrow(new DataIntegrityViolationException("dup-1"))
                .willThrow(new DataIntegrityViolationException("dup-2"));

        assertThatThrownBy(() -> memberSignupService.signup(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("dup-2");

        verify(memberService, times(2)).createMember(request);
    }

    @Test
    void happyPath_callsCreateMemberExactlyOnce() {
        ResponseMemberDto expected = ResponseMemberDto.builder().id(2L).email("new@example.com").build();
        given(memberService.createMember(request)).willReturn(expected);

        ResponseMemberDto actual = memberSignupService.signup(request);

        assertThat(actual).isEqualTo(expected);
        verify(memberService, times(1)).createMember(request);
    }
}
