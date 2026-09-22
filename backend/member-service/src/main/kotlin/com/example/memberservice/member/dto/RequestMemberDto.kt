package com.example.memberservice.member.dto

import com.example.memberservice.member.entity.Role
import java.io.Serializable

data class RequestMemberDto(
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
