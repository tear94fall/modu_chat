package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.Role;

import java.util.List;
import java.util.stream.Collectors;

public class MemberDto {

    private Long id;
    private String userId;
    private String email;
    private String auth;
    private Role role;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    private List<ProfileDto> profileDtoList;

    public Long getId() { return this.id; }
    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }
    public String getAuth() { return this.auth; }
    public Role getRole() { return this.role; }
    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
    public String getWallpaperImage() { return this.wallpaperImage; }
    public List<ProfileDto> getProfileDtoList() { return this.profileDtoList; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setAuth(String auth) { this.auth = auth; }
    public void setRole(Role role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setWallpaperImage(String wallpaperImage) { this.wallpaperImage = wallpaperImage; }
    public void setProfileDtoList(List<ProfileDto> profileDtoList) { this.profileDtoList = profileDtoList; }

    public MemberDto() {

    }

    public MemberDto(String email) {
        setEmail(email);
    }

    public MemberDto(String userId, String email) {
        setUserId(userId);
        setEmail(email);
    }

    public MemberDto(Member member) {
        setId(member.getId());
        setUserId(member.getUserId());
        setEmail(member.getEmail());
        setAuth(member.getAuth());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setWallpaperImage(member.getWallpaperImage());
        setProfileDtoList(member.getProfileList() != null ? member.getProfileList().stream().map(ProfileDto::new).collect(Collectors.toList()) : null);
    }
}
