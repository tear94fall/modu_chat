package com.example.memberservice.member.dto

import com.example.memberservice.member.entity.Role
import com.example.memberservice.profile.dto.ProfileDto
import java.io.Serializable

data class ResponseMemberDto(
    var id: Long? = null,
    var userId: String? = null,
    var auth: String? = null,
    var role: Role? = null,
    var email: String? = null,
    var username: String? = null,
    var statusMessage: String? = null,
    var profileImage: String? = null,
    var wallpaperImage: String? = null,
    var profiles: List<ProfileDto>? = null,
) : Serializable {

    constructor(memberDto: MemberDto, profiles: List<ProfileDto>?) : this(
        id = memberDto.id,
        userId = memberDto.userId,
        auth = memberDto.auth,
        role = memberDto.role,
        email = memberDto.email,
        username = memberDto.username,
        statusMessage = memberDto.statusMessage,
        profileImage = memberDto.profileImage,
        wallpaperImage = memberDto.wallpaperImage,
        profiles = profiles,
    )

    companion object {
        @JvmStatic
        fun from(memberDto: MemberDto, profiles: List<ProfileDto>?): ResponseMemberDto = ResponseMemberDto(memberDto, profiles)
    }
}
