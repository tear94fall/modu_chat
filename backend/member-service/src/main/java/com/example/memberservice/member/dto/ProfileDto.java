package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.profile.Profile;
import com.example.memberservice.member.entity.profile.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto implements Serializable {

    private ProfileType profileType;
    private String value;
    private String createdDate;
    private String updatedDate;

    public ProfileDto(Profile profile) {
        this.profileType = profile.getProfileType();
        this.value = profile.getValue();
        this.createdDate = profile.getCreatedDate() != null ? profile.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
        this.updatedDate = profile.getUpdatedDate() != null ? profile.getUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }
}
