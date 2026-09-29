package com.example.profileservice.application.usecase.command

import com.example.profileservice.application.domain.entity.ProfileType

data class CreateProfileCommand(
    val memberId: Long?,
    val profileType: ProfileType?,
    val value: String?,
)
