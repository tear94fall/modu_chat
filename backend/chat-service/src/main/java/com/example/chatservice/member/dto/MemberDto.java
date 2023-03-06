package com.example.chatservice.member.dto;

import com.example.chatservice.member.Role;
import com.example.chatservice.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
