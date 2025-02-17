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
import java.util.List;

@Entity
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
    @CollectionTable(name = "friends", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "friends")
    private List<Long> friends;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<Long> profiles;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<Long> chatRoomMembers;

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

    public void addFriends(Long id) {
        this.friends.add(id);
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
                  List<Long> friends,
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
        this.friends = friends;
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
        this.friends = new ArrayList<>();
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
                .friends(new ArrayList<>())
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
