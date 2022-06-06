package com.example.modumessenger.member.entity;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import com.example.modumessenger.member.dto.MemberDto;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String userId;

    private String auth;

    @NotNull
    private String email;

    private String username;

    private String statusMessage;
    private String profileImage;

    @ElementCollection
    @CollectionTable(name = "friends", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "friends")
    private List<Long> Friends;

    @Override
    public String toString() {
        return getUserId() + ", " + getUsername() + "," + getEmail() + "," + getAuth() + "," + getStatusMessage() + "," + getProfileImage();
    }

    public Member(MemberDto memberDto) {
        setUserId(memberDto.getUserId());
        setAuth(memberDto.getAuth());
        setEmail(memberDto.getEmail());
        setUsername(memberDto.getUsername());
        setStatusMessage(memberDto.getStatusMessage());
        setProfileImage(memberDto.getProfileImage());
        this.Friends = new ArrayList<>();
    }
}
