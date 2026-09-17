package com.example.authservice.api.pub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoCodeResponse {
    private String code;
    private long expiresIn;
}
