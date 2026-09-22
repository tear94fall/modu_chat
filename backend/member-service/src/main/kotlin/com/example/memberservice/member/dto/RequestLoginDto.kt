package com.example.memberservice.member.dto

data class RequestLoginDto(
    var userId: String? = null,
    var email: String? = null,
)
