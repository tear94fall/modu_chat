package com.example.chatservice.member.dto

import com.example.chatservice.member.Role
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
) : Serializable
