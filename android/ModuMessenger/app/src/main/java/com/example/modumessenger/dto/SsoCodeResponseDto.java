package com.example.modumessenger.dto;

public class SsoCodeResponseDto {
    private String code;
    private long expiresIn;

    public String getCode() { return code; }
    public long getExpiresIn() { return expiresIn; }
}
