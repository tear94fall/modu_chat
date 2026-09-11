package com.example.authservice.api.pub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoCodeRequest {
    private String clientId;
    private String codeChallenge;
    private String codeChallengeMethod;
}
