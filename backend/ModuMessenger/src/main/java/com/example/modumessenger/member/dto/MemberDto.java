package com.example.modumessenger.member.dto;

import com.example.modumessenger.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto implements Serializable {

    private String userId;
    private String auth;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    private List<ProfileDto> profileDtoList = new ArrayList<>();

    public MemberDto(Member member) {
        setUserId(member.getUserId());
        setAuth(member.getAuth());
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
        setUsername((String) payload.get("name"));
        setStatusMessage("");
        setProfileImage((String) payload.get("picture"));
        setWallpaperImage("");
    }
}
