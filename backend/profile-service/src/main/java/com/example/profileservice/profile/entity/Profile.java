package com.example.profileservice.profile.entity;

import com.example.profileservice.global.entity.BaseTimeEntity;
import com.example.profileservice.profile.dto.ProfileDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    private Long memberId;

    private ProfileType profileType;

    private String value;

    public Profile(ProfileDto profileDto) {
        this.memberId = profileDto.getMemberId();
        this.profileType = profileDto.getProfileType();
        this.value = profileDto.getValue();
    }
}
