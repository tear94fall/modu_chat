package com.example.authservice.oauth.sso

/** 채팅 앱이 커머스 앱에 넘겨주는 1회용 코드에 묶인 정보. */
data class SsoCode(
    val sub: String,
    val targetClientId: String,
    val codeChallenge: String,
    val codeChallengeMethod: String,
)
