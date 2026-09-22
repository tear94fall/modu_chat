package com.example.memberservice.profile.dto

import java.io.Serializable

data class ProfileDto(
    var id: Long? = null,
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
    var createdDate: String? = null,
    var updatedDate: String? = null,
) : Serializable {
    companion object {
        @JvmStatic
        fun from(id: Long?, memberId: Long?, profileType: ProfileType?, value: String?, createdDate: String?, updatedDate: String?): ProfileDto =
            ProfileDto(id, memberId, profileType, value, createdDate, updatedDate)
    }
}
