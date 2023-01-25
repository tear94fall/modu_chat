package com.example.modumessenger.dto;

public class RequestLoginDto {

    private String userId;
    private String email;

    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }

    public RequestLoginDto(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
