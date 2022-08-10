package com.example.modumessenger.member.dto;

import com.example.modumessenger.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<ProfileDto> profileDtoList = new ArrayList<>();

    public MemberDto(Member member) {
        setUserId(member.getUserId());
        setAuth(member.getAuth());
        setEmail(member.getEmail());
        setUsername(member.getUsername());
        setStatusMessage(member.getStatusMessage());
        setProfileImage(member.getProfileImage());
        setProfileDtoList(member.getProfileList().stream().map(ProfileDto::new).collect(Collectors.toList()));
    }
}
