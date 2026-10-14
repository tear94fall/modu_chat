package com.example.memberservice.api.dto

import com.example.memberservice.application.usecase.command.AddMemberProfileCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원에 프로필 이력 id 를 붙이는 요청(profile-service → member-service).")
data class AddProfileDto(
    @field:Schema(description = "회원 id(member.id). 필수.", example = "11")
    var memberId: Long? = null,
    @field:Schema(description = "profile-service 가 저장한 프로필 이력 id. 필수.", example = "305")
    var profileId: Long? = null,
) {
    fun toCommand() = AddMemberProfileCommand(memberId!!, profileId!!)
}
