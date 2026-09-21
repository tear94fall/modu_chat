package com.example.authservice.oauth.google

/** 검증을 통과한 구글 ID 토큰의 클레임. */
data class GoogleAccount(val sub: String, val email: String, val name: String, val picture: String)
