package com.example.storageservice.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private Long id;
    private Long memberId;
    private ProfileType profileType;
    private String value;
    private String createdDate;
    private String updatedDate;
}

