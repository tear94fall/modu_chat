package com.example.memberservice.member.dto

import com.example.memberservice.profile.dto.ProfileDto
import com.example.memberservice.profile.dto.ProfileType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "프로필 수정 요청. 앱 API(POST /api-public/member/{userId})는 네 필드를 통째로 덮어쓰고(빠진 필드는 비워진다), " +
        "어드민 내 정보 수정은 null 인 필드를 기존 값으로 둔다.",
)
data class UpdateProfileDto(
    @field:Schema(description = "표시 이름", example = "소율")
    var username: String? = null,
    @field:Schema(description = "상태 메시지", example = "오늘도 좋은 하루")
    var statusMessage: String? = null,
    @field:Schema(description = "프로필 사진 파일 이름(storage-service 에 올린 파일)", example = "a1b2c3d4.jpg")
    var profileImage: String? = null,
    @field:Schema(description = "배경 사진 파일 이름(storage-service 에 올린 파일)", example = "e5f6a7b8.jpg")
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
