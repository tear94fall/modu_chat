package com.example.memberservice.member.entity.profile;

import com.example.memberservice.global.entity.BaseTimeEntity;
import com.example.memberservice.member.dto.ProfileDto;
import com.example.memberservice.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    private ProfileType profileType;

    private String value;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public Profile(ProfileDto profileDto) {
        this.profileType = profileDto.getProfileType();
        this.value = profileDto.getValue();
    }
}
