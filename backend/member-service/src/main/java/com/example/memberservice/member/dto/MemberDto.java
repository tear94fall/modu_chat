package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;



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
    }

    public MemberDto(GoogleAccountDto account) {
        setUserId(account.getSub());
        setEmail(account.getEmail());
        setAuth("google");
        setRole(Role.ROLE_MEMBER);
        setUsername(account.getName());
        setStatusMessage("");
        setProfileImage(account.getPicture() == null ? "" : account.getPicture());
        setWallpaperImage("");
    }

    public static MemberDto createMemberDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .auth(member.getAuth())
                .role(member.getRole())
                .email(member.getEmail())
                .username(member.getUsername())
                .statusMessage(member.getStatusMessage())
                .profileImage(member.getProfileImage())
                .wallpaperImage(member.getWallpaperImage())
                .build();
    }
}
