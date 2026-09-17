package com.example.authservice.oauth.google;

/** 검증을 통과한 구글 ID 토큰의 클레임. */
public record GoogleAccount(String sub, String email, String name, String picture) {}
