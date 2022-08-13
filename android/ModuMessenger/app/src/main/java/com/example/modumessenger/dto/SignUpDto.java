package com.example.modumessenger.dto;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.gson.annotations.SerializedName;

public class SignUpDto {
    @SerializedName("userId")
    private String userId;
    @SerializedName("email")
    private String email;
    @SerializedName("auth")
    private String auth;
    @SerializedName("username")
    private String username;
    @SerializedName("statusMessage")
    private String statusMessage;
    @SerializedName("profileImage")
    private String profileImage;
    @SerializedName("wallpaperImage")
    private String wallpaperImage;

    public SignUpDto(GoogleSignInAccount account) {
        setUserId(account.getEmail());
        setEmail(account.getEmail());
        setAuth("google");
        setUsername(account.getDisplayName());
        setStatusMessage("");
        setProfileImage(account.getPhotoUrl() == null ? null : account.getPhotoUrl().toString());
        setWallpaperImage("");
    }

    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }
    public String getAuth() { return this.auth; }
    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
    public String getWallpaperImage() { return this.wallpaperImage; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAuth(String auth) { this.auth = auth; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setProfileImage(String profileImage) { this.profileImage = (profileImage == null || profileImage.equals("") ? "" : profileImage); }
    public void setWallpaperImage(String wallpaperImage) { this.wallpaperImage = (wallpaperImage == null || wallpaperImage.equals("") ? "" : wallpaperImage); }
}
