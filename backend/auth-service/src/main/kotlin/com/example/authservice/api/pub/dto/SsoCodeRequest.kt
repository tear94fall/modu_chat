package com.example.authservice.api.pub.dto

data class SsoCodeRequest(
    var clientId: String? = null,
    var codeChallenge: String? = null,
    var codeChallengeMethod: String? = null,
)
