package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto implements Serializable {

    private String userId;
    private String auth;
    private Role role;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    private List<ProfileDto> profileDtoList = new ArrayList<>();

    public MemberDto(Member member) {
        setUserId(member.getUserId());
        setAuth(member.getAuth());
        setRole(member.getRole());
        setEmail(member.getEmail());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setWallpaperImage(member.getWallpaperImage());
        setProfileDtoList(member.getProfileList().stream().map(ProfileDto::new).collect(Collectors.toList()));
    }

    public MemberDto(Payload payload) {
        setUserId(payload.getSubject());
        setEmail(payload.getEmail());
        setAuth("google");
        setRole(Role.ROLE_MEMBER);
        setUsername((String) payload.get("name"));
        setStatusMessage("");
        setProfileImage((String) payload.get("picture"));
        setWallpaperImage("");
    }
}
