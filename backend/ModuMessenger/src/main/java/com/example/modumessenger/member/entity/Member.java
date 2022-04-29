package com.example.modumessenger.entity;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@AllArgsConstructor
public class Member extends BaseTimeEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String userId;

    @NotNull
    private String provider;

    @NotNull
    private String email;

    @NotNull
    private String nickname;

    @Column
    private String profileImage;

    @Column
    private String statusMessage;

    public Member() {

    }
}
