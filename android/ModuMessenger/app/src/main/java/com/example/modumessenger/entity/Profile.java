package com.example.modumessenger.entity;

import com.example.modumessenger.dto.ProfileDto;
import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("profileType")
    private ProfileType profileType;
    @SerializedName("value")
    private String value;

    public ProfileType getProfileType() { return this.profileType; }
    public String getValue() { return this.value; }

    public void setValue(String value) { this.value = value; }
    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }

    public Profile(ProfileDto profileDto) {
        setProfileType(profileDto.getProfileType());
        setValue(profileDto.getValue());
    }
}
