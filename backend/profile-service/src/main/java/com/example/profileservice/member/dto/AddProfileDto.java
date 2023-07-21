package com.example.profileservice.member.dto;

import com.example.profileservice.profile.entity.Profile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddProfileDto {

    private Long memberId;
    private Long profileId;

    public AddProfileDto(Profile profile) {
        this.memberId = profile.getMemberId();
        this.profileId = profile.getId();
    }
}
