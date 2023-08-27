package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Member;

public class UpdateProfileDto {
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;

    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
    public String getWallpaperImage() { return this.wallpaperImage; }

    public void setUsername(String username) { this.username = username; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setWallpaperImage(String wallpaperImage) { this.wallpaperImage = wallpaperImage; }

    public UpdateProfileDto(MemberDto memberDto) {
        setUsername(memberDto.getUsername());
        setStatusMessage(memberDto.getStatusMessage());
        setProfileImage(memberDto.getProfileImage());
        setWallpaperImage(memberDto.getWallpaperImage());
    }

    public UpdateProfileDto(Member member) {
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setWallpaperImage(member.getWallpaperImage());
    }
}
