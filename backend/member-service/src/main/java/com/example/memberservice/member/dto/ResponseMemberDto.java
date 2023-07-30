package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.Role;
import com.example.memberservice.profile.dto.ProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMemberDto implements Serializable {

    private Long id;
    private String userId;
    private String auth;
    private Role role;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    private List<ProfileDto> profiles;

    public ResponseMemberDto(MemberDto memberDto, List<ProfileDto> profiles) {
        this.id = memberDto.getId();
        this.userId = memberDto.getUserId();
        this.auth = memberDto.getAuth();
        this.role = memberDto.getRole();
        this.email = memberDto.getEmail();
        this.username = memberDto.getUsername();
        this.statusMessage = memberDto.getStatusMessage();
        this.profileImage = memberDto.getProfileImage();
        this.wallpaperImage = memberDto.getWallpaperImage();
        this.profiles = profiles;
    }
}
