package com.example.modumessenger.dto;

public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getAccessToken() { return this.accessToken; }
    public String getRefreshToken() { return this.refreshToken; }
}
