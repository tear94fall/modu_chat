package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Profile;
import com.example.modumessenger.entity.ProfileType;

public class ProfileDto {
    private ProfileType profileType;
    private String value;

    public ProfileType getProfileType() { return this.profileType; }
    public String getValue() { return this.value; }

    public void setValue(String value) { this.value = value; }
    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }

    public ProfileDto(Profile profile) {
        setProfileType(profile.getProfileType());
        setValue(profile.getValue());
    }
}
