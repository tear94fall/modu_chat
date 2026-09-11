package com.example.modumessenger.dto;

import com.google.gson.annotations.SerializedName;

/** auth-service /oauth2/token 응답 */
public class TokenResponseDto {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("refresh_token")
    private String refreshToken;
    @SerializedName("expires_in")
    private long expiresIn;
    @SerializedName("token_type")
    private String tokenType;

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getAccessToken() { return this.accessToken; }
    public String getRefreshToken() { return this.refreshToken; }
    public long getExpiresIn() { return expiresIn; }
    public String getTokenType() { return tokenType; }
}
