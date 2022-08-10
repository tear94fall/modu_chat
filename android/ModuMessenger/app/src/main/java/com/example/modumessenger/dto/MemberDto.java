package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Member;

import java.util.List;
import java.util.stream.Collectors;

public class MemberDto {
    private String userId;
    private String email;
    private String auth;
    private String username;
    private String statusMessage;
    private String profileImage;
    private List<ProfileDto> profileDtoList;

    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }
    public String getAuth() { return this.auth; }
    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
    public List<ProfileDto> getProfileDtoList() { return this.profileDtoList; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setAuth(String auth) { this.auth = auth; }
    public void setUsername(String username) { this.username = username; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
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
        setUserId(member.getUserId());
        setEmail(member.getEmail());
        setAuth(member.getAuth());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setProfileDtoList(member.getProfileList().stream().map(ProfileDto::new).collect(Collectors.toList()));
    }
}
