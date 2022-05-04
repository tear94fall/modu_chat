package com.example.modumessenger.dto;

import com.google.gson.annotations.SerializedName;

public class MemberDto {
    private String userId;
    private String email;
    private String auth;
    private String username;
    private String statusMessage;
    private String profileImage;

    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }
    public String getAuth() { return this.auth; }
    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
}
