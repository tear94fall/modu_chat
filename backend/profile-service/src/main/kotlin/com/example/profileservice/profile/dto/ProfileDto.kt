package com.example.profileservice.profile.dto

import com.example.profileservice.profile.entity.Profile
import com.example.profileservice.profile.entity.ProfileType
import java.time.format.DateTimeFormatter

data class ProfileDto(
    var id: Long? = null,
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
    var createdDate: String? = null,
    var updatedDate: String? = null,
) {
    constructor(profile: Profile) : this(
        id = profile.id,
        memberId = profile.memberId,
        profileType = profile.profileType,
        value = profile.value,
        createdDate = profile.createdDate?.format(FORMAT) ?: "",
        updatedDate = profile.updatedDate?.format(FORMAT) ?: "",
    )

    companion object {
        private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
