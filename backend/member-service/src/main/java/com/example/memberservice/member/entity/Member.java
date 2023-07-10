package com.example.memberservice.member.entity;

import com.example.memberservice.chat.entity.ChatRoomMember;
import com.example.memberservice.global.entity.BaseTimeEntity;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.entity.profile.Profile;
import com.example.memberservice.member.entity.profile.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
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

    public void checkProfileUpdate(MemberDto memberDto) {
        if (!getStatusMessage().equals(memberDto.getStatusMessage())) {
            Profile profile = getProfileList().stream()
                    .filter(p -> p.getProfileType().equals(ProfileType.PROFILE_STATUS_MESSAGE) && p.getValue().equals(memberDto.getStatusMessage()))
                    .findFirst().orElse(null);

            if (profile != null) {
                getProfileList().remove(profile);
            }

            setStatusMessage(memberDto.getStatusMessage());
            profile = new Profile(ProfileType.PROFILE_STATUS_MESSAGE, memberDto.getStatusMessage());
            profile.setMember(this);
            insertProfile(profile);
        }

        if (!getProfileImage().equals(memberDto.getProfileImage())) {
            setProfileImage(memberDto.getProfileImage());
            Profile profile = new Profile(ProfileType.PROFILE_IMAGE, memberDto.getProfileImage());
            profile.setMember(this);
            insertProfile(profile);
        }

        if (!getWallpaperImage().equals(memberDto.getWallpaperImage())) {
            setWallpaperImage(memberDto.getWallpaperImage());
            Profile profile = new Profile(ProfileType.PROFILE_WALLPAPER, memberDto.getWallpaperImage());
            profile.setMember(this);
            insertProfile(profile);
        }
    }

    public Member update(String name, String picture) {
        this.username = name;
        this.profileImage = picture;

        return this;
    }

    public Member(MemberDto memberDto) {
        setUserId(memberDto.getUserId());
        setAuth(memberDto.getAuth());
        setRole(memberDto.getRole());
        setEmail(memberDto.getEmail());
        setUsername(memberDto.getUsername());
        setStatusMessage(memberDto.getStatusMessage());
        setProfileImage(memberDto.getProfileImage());
        setWallpaperImage(memberDto.getWallpaperImage());
        setProfileList(memberDto.getProfileDtoList().stream().map(Profile::new).collect(Collectors.toList()));
        this.Friends = new ArrayList<>();
    }

    public Member(String userId) {
        this.userId = userId;
    }

    public Member(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Member(String email, String name, String picture) {
        setEmail(email);
        setUsername(name);
        setProfileImage(picture);
    }
}
