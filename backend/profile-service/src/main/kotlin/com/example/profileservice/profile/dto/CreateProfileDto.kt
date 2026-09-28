package com.example.profileservice.profile.dto

import com.example.profileservice.profile.entity.ProfileType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 기록 추가 요청. 상태 메시지·배경·프로필 사진을 바꿀 때마다 한 행씩 쌓인다")
data class CreateProfileDto(
    @field:Schema(description = "회원 id(숫자 PK)", example = "11")
    var memberId: Long? = null,
    @field:Schema(description = "종류. PROFILE_STATUS_MESSAGE(상태 메시지) | PROFILE_WALLPAPER(배경) | PROFILE_IMAGE(프로필 사진)", example = "PROFILE_IMAGE")
    var profileType: ProfileType? = null,
    @field:Schema(description = "값. 상태 메시지면 문구, 사진·배경이면 storage-service 저장 이름", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
    var value: String? = null,
    @field:Schema(description = "받기만 하고 쓰지 않는다(생성 시각은 서버가 정한다)")
    var createdDate: String? = null,
    @field:Schema(description = "받기만 하고 쓰지 않는다")
    var updatedDate: String? = null,
)
