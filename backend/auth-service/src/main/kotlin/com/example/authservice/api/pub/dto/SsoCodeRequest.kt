package com.example.authservice.api.pub.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "SSO 1회용 코드 발급 요청. 대상 앱과 PKCE 챌린지를 담는다.")
data class SsoCodeRequest(
    @field:Schema(description = "로그인을 넘겨받을 앱의 클라이언트 ID. sso_code grant 가 있는 클라이언트여야 한다(아니면 400).", example = "modu-commerce")
    var clientId: String? = null,
    @field:Schema(
        description = "PKCE code_challenge. code_verifier 의 SHA-256 을 base64url(패딩 없음)로 인코딩한 값, 43~128자.",
        example = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
    )
    var codeChallenge: String? = null,
    @field:Schema(description = "PKCE 방식. S256 만 받는다(그 밖의 값은 400).", example = "S256")
    var codeChallengeMethod: String? = null,
)
