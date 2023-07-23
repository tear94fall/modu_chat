package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto implements Serializable {

    private Long id;
    private String userId;
    private String auth;
    private Role role;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    private List<Long> profiles;
    private List<Long> chatRoomMembers;

    public MemberDto(Member member) {
        setId(member.getId());
        setUserId(member.getUserId());
        setAuth(member.getAuth());
        setRole(member.getRole());
        setEmail(member.getEmail());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setWallpaperImage(member.getWallpaperImage());
        setProfiles(member.getProfiles());
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
