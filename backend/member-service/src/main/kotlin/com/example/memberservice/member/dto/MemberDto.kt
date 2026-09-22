package com.example.memberservice.member.dto

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.Role
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

    constructor(member: Member) : this(
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

    constructor(account: GoogleAccountDto) : this(
        userId = account.sub,
        email = account.email,
        auth = "google",
        role = Role.ROLE_MEMBER,
        username = account.name,
        statusMessage = "",
        profileImage = account.picture ?: "",
        wallpaperImage = "",
    )

    companion object {
        @JvmStatic
        fun createMemberDto(member: Member): MemberDto = MemberDto(member)
    }
}
