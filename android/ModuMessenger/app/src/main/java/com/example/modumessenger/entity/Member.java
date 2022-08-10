package com.example.modumessenger.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.example.modumessenger.dto.MemberDto;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.stream.Collectors;

public class Member implements Parcelable {
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
    @SerializedName("profile")
    private List<Profile> profileList;

    public Member(String userId, Member member) {

    }

    public Member(MemberDto memberDto) {
        setUserId(memberDto.getEmail());
        setEmail(memberDto.getEmail());
        setAuth("google");
        setUsername(memberDto.getUsername());
        setStatusMessage(memberDto.getStatusMessage());
        setProfileImage(memberDto.getProfileImage() == null ? null : memberDto.getProfileImage().toString());
        setProfileList(memberDto.getProfileDtoList().stream().map(Profile::new).collect(Collectors.toList()));
    }

    public Member(GoogleSignInAccount account) {
        setUserId(account.getEmail());
        setEmail(account.getEmail());
        setAuth("google");
        setUsername(account.getDisplayName());
        setStatusMessage("");
        setProfileImage(account.getPhotoUrl() == null ? null : account.getPhotoUrl().toString());
    }

    public Member(String email) {
        this.userId = null;
        this.email = email;
        this.auth = null;
        this.username = null;
        this.statusMessage = null;
        this.profileImage = null;
    }

    public Member(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    protected Member(Parcel in) {
        userId = in.readString();
        email = in.readString();
    }

    public String getUserId() { return this.userId; }
    public String getEmail() { return this.email; }
    public String getAuth() { return this.auth; }
    public String getUsername() { return this.username; }
    public String getStatusMessage() { return this.statusMessage; }
    public String getProfileImage() { return this.profileImage; }
    public List<Profile> getProfileList() { return this.profileList; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAuth(String auth) { this.auth = auth; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage == null || profileImage.equals("") ? "" : profileImage; }
    public void setProfileList(List<Profile> profileList) { this.profileList = profileList; }

    @NonNull
    @Override
    public String toString() { return "Member{" + "user_id='" + userId + '\'' + ", email='" + email + '\'' + '}'; }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel in) {
            return new Member(in);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(userId);
        parcel.writeString(email);
    }
}