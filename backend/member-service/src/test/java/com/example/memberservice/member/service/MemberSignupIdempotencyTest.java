package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.storage.client.StorageFeignClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재설치·기기 변경 시 이미 가입된 이메일이면 예외 대신 기존 회원을 그대로 돌려줘야 한다는
 * 멱등성 요구사항을 검증한다. GoogleIdTokenValidator 를 목킹해 실제 구글 네트워크 호출 없이
 * Payload 를 주입한다.
 */
@SpringBootTest
@Transactional
class MemberSignupIdempotencyTest {

    private static final String UPLOADED_FILE = "uploaded-profile.png";

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private GoogleIdTokenValidator googleIdTokenValidator;

    @MockitoBean
    private StorageFeignClient storageFeignClient;

    @MockitoBean
    private ProfileFeignClient profileFeignClient;

    @Test
    void signingUpTwice_withSameEmail_createsOnlyOneMember() {
        String email = "returning-" + UUID.randomUUID() + "@example.com";
        stubGoogleVerification(email, "google-sub-" + UUID.randomUUID());
        stubProfileCollaborators();

        long before = memberRepository.count();

        var first = memberService.createMember(googleLoginRequest("token-1"));
        var second = memberService.createMember(googleLoginRequest("token-2"));

        long after = memberRepository.count();

        assertThat(after - before).isEqualTo(1);
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEmail()).isEqualTo(email);
    }

    @Test
    void secondSignup_doesNotRepeatProfileImageCreation() {
        String email = "returning-" + UUID.randomUUID() + "@example.com";
        stubGoogleVerification(email, "google-sub-" + UUID.randomUUID());
        stubProfileCollaborators();

        memberService.createMember(googleLoginRequest("token-1"));
        clearInvocations(storageFeignClient, profileFeignClient);

        memberService.createMember(googleLoginRequest("token-2"));

        verify(storageFeignClient, never()).upload(anyString());
        verify(profileFeignClient, never()).addProfileRequest(any());
    }

    @Test
    void unknownEmail_createsNewMember() {
        String email = "brand-new-" + UUID.randomUUID() + "@example.com";
        stubGoogleVerification(email, "google-sub-" + UUID.randomUUID());
        stubProfileCollaborators();

        var response = memberService.createMember(googleLoginRequest("token-only"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(memberRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void invalidOrExpiredToken_doesNotNpe_throwsUnauthorized() {
        given(googleIdTokenValidator.verify(anyString())).willReturn(null);

        assertThatThrownBy(() -> memberService.createMember(googleLoginRequest("expired-token")))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR);
    }

    private void stubGoogleVerification(String email, String subject) {
        Payload payload = new Payload();
        payload.setEmail(email);
        payload.setSubject(subject);
        payload.set("name", "테스트유저");
        payload.set("picture", "https://example.com/picture.jpg");

        given(googleIdTokenValidator.verify(anyString())).willReturn(payload);
    }

    private void stubProfileCollaborators() {
        given(storageFeignClient.upload(anyString())).willReturn(ResponseEntity.ok(UPLOADED_FILE));
        given(profileFeignClient.addProfileRequest(any())).willAnswer(invocation -> {
            ProfileDto request = invocation.getArgument(0);
            return ResponseEntity.ok(ProfileDto.from(1L, request.getMemberId(), request.getProfileType(), UPLOADED_FILE, "", ""));
        });
        given(profileFeignClient.getMemberProfiles(anyLong())).willReturn(ResponseEntity.ok(List.of()));
    }

    private GoogleLoginRequest googleLoginRequest(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setAuthType("google");
        request.setIdToken(idToken);
        return request;
    }
}
