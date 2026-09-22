package com.example.authservice.api.pub.dto

data class SsoCodeResponse(
    var code: String? = null,
    var expiresIn: Long = 0,
)
