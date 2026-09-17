package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.memberservice.member.dto.GoogleAccountDto;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.storage.client.StorageFeignClient;
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
 * 멱등성 요구사항을 검증한다. 구글 검증은 auth-service 가 하므로 여기서는 검증된 계정 DTO 를 바로 넣는다.
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
    private StorageFeignClient storageFeignClient;

    @MockitoBean
    private ProfileFeignClient profileFeignClient;

    @Test
    void signingUpTwice_withSameEmail_createsOnlyOneMember() {
        String email = "returning-" + UUID.randomUUID() + "@example.com";
        GoogleAccountDto account = new GoogleAccountDto("google-sub-" + UUID.randomUUID(), email, "테스트유저", "https://example.com/picture.jpg");
        stubProfileCollaborators();

        long before = memberRepository.count();

        var first = memberService.findOrCreateGoogleMember(account);
        var second = memberService.findOrCreateGoogleMember(account);

        long after = memberRepository.count();

        assertThat(after - before).isEqualTo(1);
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEmail()).isEqualTo(email);
    }

    @Test
    void secondSignup_doesNotRepeatProfileImageCreation() {
        String email = "returning-" + UUID.randomUUID() + "@example.com";
        GoogleAccountDto account = new GoogleAccountDto("google-sub-" + UUID.randomUUID(), email, "테스트유저", "https://example.com/picture.jpg");
        stubProfileCollaborators();

        memberService.findOrCreateGoogleMember(account);
        clearInvocations(storageFeignClient, profileFeignClient);

        memberService.findOrCreateGoogleMember(account);

        verify(storageFeignClient, never()).upload(anyString());
        verify(profileFeignClient, never()).addProfileRequest(any());
    }

    @Test
    void unknownEmail_createsNewMember() {
        String email = "brand-new-" + UUID.randomUUID() + "@example.com";
        GoogleAccountDto account = new GoogleAccountDto("google-sub-" + UUID.randomUUID(), email, "테스트유저", "https://example.com/picture.jpg");
        stubProfileCollaborators();

        var response = memberService.findOrCreateGoogleMember(account);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(memberRepository.existsByEmail(email)).isTrue();
    }

    private void stubProfileCollaborators() {
        given(storageFeignClient.upload(anyString())).willReturn(ResponseEntity.ok(UPLOADED_FILE));
        given(profileFeignClient.addProfileRequest(any())).willAnswer(invocation -> {
            ProfileDto request = invocation.getArgument(0);
            return ResponseEntity.ok(ProfileDto.from(1L, request.getMemberId(), request.getProfileType(), UPLOADED_FILE, "", ""));
        });
        given(profileFeignClient.getMemberProfiles(anyLong())).willReturn(ResponseEntity.ok(List.of()));
    }

}
