package com.example.modumessenger.member.entity.profile;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@Setter
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue
    private Long id;

    private ProfileType profileType;

    private String value;
}
