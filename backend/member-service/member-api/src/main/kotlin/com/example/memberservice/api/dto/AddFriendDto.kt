package com.example.memberservice.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "친구 추가 요청.")
data class AddFriendDto(
    @field:Schema(description = "추가할 회원의 이메일. 필수, 가입한 회원이어야 한다.", example = "soyul@example.com")
    var email: String? = null,
)
