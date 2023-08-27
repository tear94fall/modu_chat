package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Profile;
import com.example.modumessenger.entity.ProfileType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProfileDto {
    private Long id;
    private Long memberId;
    private ProfileType profileType;
    private String value;
    private String createdDate;
    private String updatedDate;

    public Long getId() { return this.id; }
    public Long getMemberId() { return this.memberId; }
    public ProfileType getProfileType() { return this.profileType; }
    public String getValue() { return this.value; }
    public String getCreatedDate() { return this.createdDate; }
    public String getUpdatedDate() { return this.updatedDate; }

    public void setId(Long id) { this.id = id; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public void setValue(String value) { this.value = value; }
    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public void setUpdatedDate(String updatedDate) { this.updatedDate = updatedDate; }

    public ProfileDto(Long id, Long memberId, ProfileType type, String value, String createdDate, String updatedDate) {
        setId(id);
        setMemberId(memberId);
        setProfileType(type);
        setValue(value);
        if (!createdDate.equals("")) { setCreatedDate(createdDate); }
        if (!updatedDate.equals("")) { setUpdatedDate(updatedDate); }
    }

    public ProfileDto(Profile profile) {
        setId(profile.getId());
        setMemberId(profile.getMemberId());
        setProfileType(profile.getProfileType());
        setValue(profile.getValue());
        setCreatedDate(profile.getCreatedDate() != null ? profile.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        setUpdatedDate(profile.getUpdatedDate() != null ? profile.getUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
    }

    public String getLastModifiedDate() {
        return this.updatedDate != null ? this.updatedDate : this.createdDate;
    }

    public LocalDateTime getLastModifiedDateTime() {
        return this.getUpdatedLocalDateTime() != null ? this.getUpdatedLocalDateTime() : getCreatedLocalDateTime();
    }

    public LocalDateTime getCreatedLocalDateTime() {
        if(this.createdDate != null && !this.createdDate.equals("")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(this.createdDate, formatter);
        }

        return null;
    }

    public LocalDateTime getUpdatedLocalDateTime() {
        if(this.updatedDate != null && !this.updatedDate.equals("")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(this.updatedDate, formatter);
        }

        return null;
    }
}
