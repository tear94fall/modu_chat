package com.example.memberservice.member.entity;

import com.example.memberservice.chat.entity.ChatRoomMember;
import com.example.memberservice.global.entity.BaseTimeEntity;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.dto.UpdateProfileDto;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.profile.dto.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
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

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ChatRoomMember> chatRoomMemberList = new ArrayList<>();

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
