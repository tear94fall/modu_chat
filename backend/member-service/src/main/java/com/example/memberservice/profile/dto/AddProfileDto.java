package com.example.memberservice.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddProfileDto {

    private Long memberId;
    private Long profileId;
}
