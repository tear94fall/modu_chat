package com.example.authservice.oauth.google

/** 검증을 통과한 구글 ID 토큰의 클레임. */
/** [emailVerified] 는 구글이 이메일 소유를 확인했는지. 직원 로그인은 이메일로 사람을 찾으므로 확인된 이메일만 받는다. */
data class GoogleAccount(val sub: String, val email: String, val name: String, val picture: String, val emailVerified: Boolean = true)
