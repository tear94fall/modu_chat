package com.example.chatservice.api.dto

import com.example.chatservice.application.member.MemberInfo
import com.example.chatservice.application.member.Role
import java.io.Serializable

/** 방 멤버의 회원 정보. member-service 가 준 모양 그대로 앱에 내려간다. */
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

    fun toInfo() = MemberInfo(id, userId, auth, role, email, username, statusMessage, profileImage, wallpaperImage)

    companion object {
        fun of(member: MemberInfo) = MemberDto(
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
