package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto;
import com.example.memberservice.member.dto.UpdateProfileDto;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Test
    void updateMyProfile_nullFieldsKeepExisting_emptyStringsClear() {
        String userId = "user-" + UUID.randomUUID();
        Member member = Member.builder()
                .userId(userId)
                .auth("google")
                .email(userId + "@example.com")
                .username("기존이름")
                .statusMessage("기존상태")
                .profileImage("profile.jpg")
                .wallpaperImage("wallpaper.jpg")
                .friends(new ArrayList<>())
                .profiles(new ArrayList<>())
                .chatRoomMembers(new ArrayList<>())
                .build();
        memberRepository.save(member);

        UpdateProfileDto request = UpdateProfileDto.builder()
                .username("새이름")
                .statusMessage(null)
                .profileImage("")
                .wallpaperImage(null)
                .build();

        AdminMemberDetailDto result = memberService.updateMyProfile(userId, request);

        assertThat(result.getMember().getUsername()).isEqualTo("새이름");
        assertThat(result.getMember().getStatusMessage()).isEqualTo("기존상태");
        assertThat(result.getMember().getProfileImage()).isEqualTo("");
        assertThat(result.getMember().getWallpaperImage()).isEqualTo("wallpaper.jpg");
    }
}