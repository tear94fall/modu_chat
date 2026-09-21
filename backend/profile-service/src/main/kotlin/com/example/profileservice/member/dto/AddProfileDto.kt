package com.example.profileservice.member.dto

import com.example.profileservice.profile.entity.Profile

data class AddProfileDto(val memberId: Long?, val profileId: Long?) {
    constructor(profile: Profile) : this(profile.memberId, profile.id)
}
