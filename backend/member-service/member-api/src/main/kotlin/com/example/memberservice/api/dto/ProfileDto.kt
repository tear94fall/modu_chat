package com.example.memberservice.api.dto

import com.example.memberservice.application.domain.entity.ProfileType
import com.example.memberservice.application.port.ProfileInfo
import java.io.Serializable

/** profile-service 의 프로필 이력 한 건. Feign 요청·응답, Kafka 되돌리기 메시지, 앱 응답(profiles)에 같은 모양으로 쓴다. */
data class ProfileDto(
    var id: Long? = null,
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
    var createdDate: String? = null,
    var updatedDate: String? = null,
) : Serializable {

    fun toInfo() = ProfileInfo(id, memberId, profileType, value, createdDate, updatedDate)

    companion object {
        @JvmStatic
        fun from(id: Long?, memberId: Long?, profileType: ProfileType?, value: String?, createdDate: String?, updatedDate: String?): ProfileDto =
            ProfileDto(id, memberId, profileType, value, createdDate, updatedDate)

        fun of(info: ProfileInfo) = ProfileDto(info.id, info.memberId, info.profileType, info.value, info.createdDate, info.updatedDate)
    }
}
