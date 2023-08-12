package com.example.profileservice.profile.dto;

import com.example.profileservice.profile.entity.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateProfileDto {

    private Long memberId;
    private ProfileType profileType;
    private String value;
    private String createdDate;
    private String updatedDate;
}
