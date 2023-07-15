package com.example.profileservice.profile.dto;

import com.example.profileservice.profile.entity.Profile;
import com.example.profileservice.profile.entity.ProfileType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
public class ProfileDto {

    private Long memberId;
    private ProfileType profileType;
    private String value;
    private String createdDate;
    private String updatedDate;

    public ProfileDto(Profile profile) {
        this.memberId = profile.getMemberId();
        this.profileType = profile.getProfileType();
        this.value = profile.getValue();
        this.createdDate = profile.getCreatedDate() != null ? profile.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
        this.updatedDate = profile.getUpdatedDate() != null ? profile.getUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }
}
