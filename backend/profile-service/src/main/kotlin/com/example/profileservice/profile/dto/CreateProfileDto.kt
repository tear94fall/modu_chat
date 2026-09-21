package com.example.profileservice.profile.dto

import com.example.profileservice.profile.entity.ProfileType

data class CreateProfileDto(
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
    var createdDate: String? = null,
    var updatedDate: String? = null,
)
