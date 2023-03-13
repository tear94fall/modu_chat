package com.example.memberservice.member.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class GoogleLoginRequest {

    private String authType;
    private String idToken;
}
