package com.example.modumessenger.member.entity.profile;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import com.example.modumessenger.member.dto.ProfileDto;
import com.example.modumessenger.member.entity.Member;
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
