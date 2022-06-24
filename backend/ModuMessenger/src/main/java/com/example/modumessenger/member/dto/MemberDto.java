package com.example.modumessenger.member.dto;

import com.example.modumessenger.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto implements Serializable {

    private String userId;
    private String auth;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;

    public MemberDto(Member member) {
        setUserId(member.getUserId());
        setAuth(member.getAuth());
        setEmail(member.getEmail());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
    }
}
