package com.example.authservice.oauth.sso;

/** 채팅 앱이 커머스 앱에 넘겨주는 1회용 코드에 묶인 정보. */
public record SsoCode(String sub, String targetClientId, String codeChallenge, String codeChallengeMethod) {}
