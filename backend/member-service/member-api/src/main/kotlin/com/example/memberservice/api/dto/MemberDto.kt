package com.example.memberservice.api.dto

import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.usecase.result.MemberResult
import java.io.Serializable

data class MemberDto(
    var id: Long? = null,
    var userId: String? = null,
    var auth: String? = null,
    var role: Role? = null,
    var email: String? = null,
    var username: String? = null,
    var statusMessage: String? = null,
    var profileImage: String? = null,
    var wallpaperImage: String? = null,
) : Serializable {

    companion object {
        fun of(member: MemberResult): MemberDto = MemberDto(
            id = member.id,
            userId = member.userId,
            auth = member.auth,
            role = member.role,
            email = member.email,
            username = member.username,
            statusMessage = member.statusMessage,
            profileImage = member.profileImage,
            wallpaperImage = member.wallpaperImage,
        )
    }
}
