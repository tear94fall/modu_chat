package com.example.memberservice.member.entity;

import com.example.memberservice.global.entity.BaseTimeEntity;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.dto.UpdateProfileDto;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.profile.dto.ProfileType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_member_user_id", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id")
    private String userId;

    private String auth;

    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    private String email;

    private String username;

    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<Long> profiles;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<Long> chatRoomMembers;

    /** 탈퇴한 회원의 표시 이름. 남은 사람들의 대화·방 멤버 목록에서 이렇게 보인다. */
    public static final String WITHDRAWN_USERNAME = "탈퇴한 회원";

    /** VARCHAR DEFAULT 'ACTIVE' 라 ddl-auto: update 로 컬럼이 붙어도 기존 행은 ACTIVE 다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(16) DEFAULT 'ACTIVE'")
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "withdrawn_date")
    private LocalDateTime withdrawnDate;

    public boolean isWithdrawn() {
        return status == MemberStatus.WITHDRAWN;
    }

    /**
     * 탈퇴. 행은 남기되 개인정보는 비운다. userId 는 채팅 기록과의 연결이라 그대로 두고,
     * 이메일은 유일 제약(NOT NULL)이 있어 자리표시자로 바꿔 같은 구글 계정이 다시 가입할 수 있게 한다.
     */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnDate = LocalDateTime.now();
        this.email = "withdrawn:" + userId;
        this.username = WITHDRAWN_USERNAME;
        this.statusMessage = "";
        this.profileImage = "";
        this.wallpaperImage = "";
        if (this.profiles != null) this.profiles.clear();
        if (this.chatRoomMembers != null) this.chatRoomMembers.clear();
    }

    /** 탈퇴했던 사람이 같은 구글 계정으로 다시 로그인하면 같은 행을 되살린다(채팅 기록의 userId 가 그대로 이어진다). */
    public void reactivate(String email, String username) {
        this.status = MemberStatus.ACTIVE;
        this.withdrawnDate = null;
        this.email = email;
        this.username = username;
        this.statusMessage = "";
        this.profileImage = "";
        this.wallpaperImage = "";
        this.role = Role.ROLE_MEMBER;
    }

    public void addProfile(Long id) {
        this.profiles.add(id);
    }

    public void addChatRoom(Long id) {
        this.chatRoomMembers.add(id);
    }

    public void delChatRoom(Long id) {
        this.chatRoomMembers.remove(id);
    }

    @Override
    public String toString() {
        return getUserId() + ", " + getUsername() + "," + getEmail() + "," + getAuth() + "," + getStatusMessage() + "," + getProfileImage();
    }

    public void updateMemberInfo(ProfileDto profileDto) {
        if (profileDto.getProfileType() == ProfileType.PROFILE_STATUS_MESSAGE) {
            this.statusMessage = profileDto.getValue();
        } else if (profileDto.getProfileType() == ProfileType.PROFILE_IMAGE) {
            this.profileImage = profileDto.getValue();
        } else if (profileDto.getProfileType() == ProfileType.PROFILE_WALLPAPER) {
            this.wallpaperImage = profileDto.getValue();
        }
    }

    public void updateProfile(UpdateProfileDto updateProfileDto) {
        this.username = updateProfileDto.getUsername();
        this.statusMessage = updateProfileDto.getStatusMessage();
        this.profileImage = updateProfileDto.getProfileImage();
        this.wallpaperImage = updateProfileDto.getWallpaperImage();
    }

    @Builder
    public Member(String userId,
                  String auth,
                  Role role,
                  String email,
                  String username,
                  String statusMessage,
                  String profileImage,
                  String wallpaperImage,
                  List<Long> profiles,
                  List<Long> chatRoomMembers
        ) {
        this.userId = userId;
        this.auth = auth;
        this.role = role;
        this.email = email;
        this.username = username;
        this.statusMessage = statusMessage;
        this.profileImage = profileImage;
        this.wallpaperImage = wallpaperImage;
        this.profiles = profiles;
        this.chatRoomMembers = chatRoomMembers;
    }

    public Member(MemberDto memberDto) {
        this.userId = memberDto.getUserId();
        this.auth = memberDto.getAuth();
        this.role = memberDto.getRole();
        this.email = memberDto.getEmail();
        this.username = memberDto.getUsername();
        this.statusMessage = memberDto.getStatusMessage();
        this.profileImage = memberDto.getProfileImage();
        this.wallpaperImage = memberDto.getWallpaperImage();
        this.profiles = new ArrayList<>();
        this.chatRoomMembers = new ArrayList<>();
    }

    public static Member createMember(MemberDto memberDto) {
        return Member.builder()
                .userId(memberDto.getUserId())
                .auth(memberDto.getAuth())
                .role(memberDto.getRole())
                .email(memberDto.getEmail())
                .username(memberDto.getUsername())
                .statusMessage(memberDto.getStatusMessage())
                .profileImage(memberDto.getProfileImage())
                .wallpaperImage(memberDto.getWallpaperImage())
                .profiles(new ArrayList<>())
                .chatRoomMembers(new ArrayList<>())
                .build();
    }

    public Member(String userId) {
        this.userId = userId;
    }

    public Member(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Member(String email, String username, String picture) {
        this.email = email;
        this.username = username;
        this.profileImage = picture;
    }
}
