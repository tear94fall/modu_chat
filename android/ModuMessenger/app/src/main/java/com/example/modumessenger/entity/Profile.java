package com.example.modumessenger.entity;

import com.example.modumessenger.dto.ProfileDto;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Profile {

    private Long id;
    @SerializedName("profileType")
    private ProfileType profileType;
    @SerializedName("value")
    private String value;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public Long getId() { return this.id; }
    public ProfileType getProfileType() { return this.profileType; }
    public String getValue() { return this.value; }
    public LocalDateTime getCreatedDate() { return this.createdDate; }
    public LocalDateTime getUpdatedDate() { return this.updatedDate; }

    public void setId(Long id) { this.id = id; }
    public void setValue(String value) { this.value = value; }
    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public Profile(ProfileDto profileDto) {
        setId(profileDto.getId());
        setProfileType(profileDto.getProfileType());
        setValue(profileDto.getValue());
        setCreatedDate(profileDto.getCreatedLocalDateTime());
        setUpdatedDate(profileDto.getUpdatedLocalDateTime());
    }

    public String getLastModifiedDate() {
        return this.getUpdatedDateTime() != null ? this.getUpdatedDateTime() : this.getCreatedDateTime();
    }

    public LocalDateTime getLastModifiedDateTime() {
        return this.updatedDate != null ? this.updatedDate : this.createdDate;
    }

    public String getCreatedDateTime() {
        return this.createdDate != null ? this.createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    public String getUpdatedDateTime() {
        return this.updatedDate != null ? this.updatedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
}
