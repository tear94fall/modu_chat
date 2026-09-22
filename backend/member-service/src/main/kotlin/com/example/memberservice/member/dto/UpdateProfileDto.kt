package com.example.memberservice.member.dto

import com.example.memberservice.profile.dto.ProfileDto
import com.example.memberservice.profile.dto.ProfileType

data class UpdateProfileDto(
    var username: String? = null,
    var statusMessage: String? = null,
    var profileImage: String? = null,
    var wallpaperImage: String? = null,
) {
    companion object {
        @JvmStatic
        fun createUpdateProfileDto(memberDto: MemberDto, profileDto: ProfileDto): UpdateProfileDto {
            val profileImage = if (profileDto.profileType == ProfileType.PROFILE_IMAGE) profileDto.value else memberDto.profileImage
            val wallpaperImage = if (profileDto.profileType == ProfileType.PROFILE_WALLPAPER) profileDto.value else memberDto.wallpaperImage

            return UpdateProfileDto(
                username = memberDto.username,
                statusMessage = memberDto.statusMessage,
                profileImage = profileImage,
                wallpaperImage = wallpaperImage,
            )
        }
    }
}
