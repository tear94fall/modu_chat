package com.example.modumessenger.dto;

import com.example.modumessenger.entity.ProfileType;

public class CreateProfileDto {

    private Long memberId;
    private ProfileType profileType;
    private String value;

    public Long getMemberId() { return this.memberId; }
    public ProfileType getProfileType() { return this.profileType; }
    public String getValue() { return this.value; }

    public void setMemberId(Long id) { this.memberId = id; }
    public void setProfileType(ProfileType type) { this.profileType = type; }
    public void setValue(String value) { this.value = value; }

    public CreateProfileDto(Long id, ProfileType type, String value) {
        this.memberId = id;
        this.profileType = type;
        this.value = value;
    }
}
