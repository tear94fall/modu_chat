package com.example.authservice.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 구글 ID 토큰을 검증한 결과. member-service 내부 API 로 넘겨 회원을 찾거나 만든다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAccountDto {
    private String sub;
    private String email;
    private String name;
    private String picture;
}
