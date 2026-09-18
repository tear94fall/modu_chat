package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.memberservice.chat.client.ChatFeignClient;
import com.example.memberservice.member.dto.GoogleAccountDto;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.entity.MemberStatus;
import com.example.memberservice.member.repository.MemberFriendRepository;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.notice.client.PushFeignClient;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.storage.client.StorageFeignClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 회원 탈퇴: 다른 서비스 정리를 요청하고, 친구 관계를 지우고, 회원 행은 개인정보만 비운 채 남긴다. */
@SpringBootTest
class MemberWithdrawTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberFriendRepository memberFriendRepository;

    @MockitoBean ChatFeignClient chatFeignClient;
    @MockitoBean PushFeignClient pushFeignClient;
    @MockitoBean StorageFeignClient storageFeignClient;
    @MockitoBean ProfileFeignClient profileFeignClient;

    private Member saveMember(String tag) {
        return memberRepository.save(Member.builder()
                .userId("sub-" + tag)
                .auth("google")
                .email(tag + "@example.com")
                .username("이름-" + tag)
                .statusMessage("상태")
                .profileImage("profile-" + tag + ".png")
                .wallpaperImage("wall-" + tag + ".jpg")
                .profiles(new ArrayList<>())
                .chatRoomMembers(new ArrayList<>(List.of(1L, 2L)))
                .build());
    }

    // 지연 로딩 컬렉션(chatRoomMembers)을 검증하려면 같은 세션 안이어야 한다.
    @Test
    @Transactional
    void withdraw_scrubsPersonalData_dropsFriendsBothWays_andCleansOtherServices() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Member me = saveMember(tag);
        Member friend = saveMember(tag + "-f");
        memberFriendRepository.save(MemberFriend.of(me, friend));
        memberFriendRepository.save(MemberFriend.of(friend, me));
        given(chatFeignClient.exitAllChatRooms(me.getId())).willReturn(List.of(1L, 2L));
        given(pushFeignClient.deleteToken(anyString())).willReturn(ResponseEntity.noContent().build());
        given(storageFeignClient.delete(anyString())).willReturn(ResponseEntity.ok("ok"));

        memberService.withdraw(me.getUserId());

        Member after = memberRepository.findById(me.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(after.getWithdrawnDate()).isNotNull();
        assertThat(after.getUserId()).isEqualTo(me.getUserId()); // 채팅 기록과의 연결은 남긴다
        assertThat(after.getEmail()).doesNotContain("@example.com"); // 같은 구글 계정으로 다시 가입할 수 있어야 한다
        assertThat(after.getUsername()).isEqualTo(Member.WITHDRAWN_USERNAME);
        assertThat(after.getStatusMessage()).isEmpty();
        assertThat(after.getProfileImage()).isEmpty();
        assertThat(after.getWallpaperImage()).isEmpty();
        assertThat(after.getChatRoomMembers()).isEmpty();

        assertThat(memberFriendRepository.findByMemberIdAndFriendId(me.getId(), friend.getId())).isEmpty();
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(friend.getId(), me.getId())).isEmpty();

        verify(chatFeignClient).exitAllChatRooms(me.getId());
        verify(pushFeignClient).deleteToken(me.getUserId());
        verify(storageFeignClient).delete("profile-" + tag + ".png");
        verify(storageFeignClient).delete("wall-" + tag + ".jpg");
    }

    @Test
    void withdraw_twice_isIdempotent() {
        Member me = saveMember(UUID.randomUUID().toString().substring(0, 8));
        given(chatFeignClient.exitAllChatRooms(anyLong())).willReturn(List.of());
        given(pushFeignClient.deleteToken(anyString())).willReturn(ResponseEntity.noContent().build());

        memberService.withdraw(me.getUserId());
        memberService.withdraw(me.getUserId());

        verify(chatFeignClient).exitAllChatRooms(me.getId()); // 두 번째는 아무것도 안 한다
    }

    @Test
    void googleSignIn_afterWithdrawal_reactivatesTheSameRow() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Member me = saveMember(tag);
        given(chatFeignClient.exitAllChatRooms(anyLong())).willReturn(List.of());
        given(pushFeignClient.deleteToken(anyString())).willReturn(ResponseEntity.noContent().build());
        given(profileFeignClient.getMemberProfiles(anyLong())).willReturn(ResponseEntity.ok(List.of()));
        memberService.withdraw(me.getUserId());

        // 같은 구글 계정(sub)으로 다시 로그인. 사진은 없다고 두어 storage 업로드 경로를 타지 않는다.
        GoogleAccountDto account = new GoogleAccountDto(me.getUserId(), tag + "@example.com", "돌아온 이름", null);
        MemberDto again = memberService.findOrCreateGoogleMember(account);

        assertThat(again.getId()).isEqualTo(me.getId());
        Member after = memberRepository.findById(me.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(after.getEmail()).isEqualTo(tag + "@example.com");
        assertThat(after.getUsername()).isEqualTo("돌아온 이름");
        assertThat(after.getWithdrawnDate()).isNull();
        verify(storageFeignClient, never()).upload(any(String.class));
    }
}
