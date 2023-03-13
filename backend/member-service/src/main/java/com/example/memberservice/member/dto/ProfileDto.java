package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.profile.Profile;
import com.example.memberservice.member.entity.profile.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto implements Serializable {

    private ProfileType profileType;
    private String value;

    public ProfileDto(Profile profile) {
        this.profileType = profile.getProfileType();
        this.value = profile.getValue();
    }
}
