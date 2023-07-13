package com.example.memberservice.member.entity;

import com.example.memberservice.chat.entity.ChatRoomMember;
import com.example.memberservice.global.entity.BaseTimeEntity;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.entity.profile.Profile;
import com.example.memberservice.member.entity.profile.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<Profile> profileList = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ChatRoomMember> chatRoomMemberList = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "friends", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "friends")
    private List<Long> Friends;

    public void insertProfile(Profile profile) {
        this.profileList.add(profile);
    }

    @Override
    public String toString() {
        return getUserId() + ", " + getUsername() + "," + getEmail() + "," + getAuth() + "," + getStatusMessage() + "," + getProfileImage();
    }

    public void updateProfile(MemberDto memberDto) {
        if (!getStatusMessage().equals(memberDto.getStatusMessage())) {
            Profile profile = getProfileList().stream()
                    .filter(p -> p.getProfileType().equals(ProfileType.PROFILE_STATUS_MESSAGE) && p.getValue().equals(memberDto.getStatusMessage()))
                    .findFirst().orElse(null);

            if (profile != null) {
                getProfileList().remove(profile);
            }

            this.statusMessage = memberDto.getStatusMessage();
            profile = new Profile(ProfileType.PROFILE_STATUS_MESSAGE, memberDto.getStatusMessage());
            profile.setMember(this);
            insertProfile(profile);
        }

        if (!getProfileImage().equals(memberDto.getProfileImage())) {
            this.profileImage = memberDto.getProfileImage();;
            Profile profile = new Profile(ProfileType.PROFILE_IMAGE, memberDto.getProfileImage());
            profile.setMember(this);
            insertProfile(profile);
        }

        if (!getWallpaperImage().equals(memberDto.getWallpaperImage())) {
            this.wallpaperImage = memberDto.getWallpaperImage();
            Profile profile = new Profile(ProfileType.PROFILE_WALLPAPER, memberDto.getWallpaperImage());
            profile.setMember(this);
            insertProfile(profile);
        }
    }

    public void updateMember(MemberDto memberDto) {
        this.username = memberDto.getUsername();
        this.statusMessage = memberDto.getStatusMessage();
        this.profileImage = memberDto.getProfileImage();
        this.wallpaperImage = memberDto.getWallpaperImage();

        this.updateProfile(memberDto);
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
        this.profileList = memberDto.getProfileDtoList().stream().map(Profile::new).collect(Collectors.toList());
        this.Friends = new ArrayList<>();
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
