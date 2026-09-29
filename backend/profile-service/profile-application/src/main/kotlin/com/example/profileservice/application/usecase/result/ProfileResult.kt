package com.example.profileservice.application.usecase.result

import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.entity.ProfileType
import java.time.LocalDateTime

/** 트랜잭션 안에서 엔티티를 옮겨 담은 값. 컨트롤러는 이것만 본다. */
data class ProfileResult(
    val id: Long?,
    val memberId: Long?,
    val profileType: ProfileType?,
    val value: String?,
    val createdDate: LocalDateTime?,
    val updatedDate: LocalDateTime?,
) {
    companion object {
        fun from(profile: Profile): ProfileResult = ProfileResult(
            id = profile.id,
            memberId = profile.memberId,
            profileType = profile.profileType,
            value = profile.value,
            createdDate = profile.createdDate,
            updatedDate = profile.updatedDate,
        )
    }
}
