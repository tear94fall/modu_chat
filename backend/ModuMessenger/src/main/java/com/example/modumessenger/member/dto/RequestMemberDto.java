package com.example.modumessenger.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMemberDto implements Serializable {

    private String userId;
    private String auth;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
}
