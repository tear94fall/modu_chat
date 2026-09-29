package com.example.memberservice.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "친구 이름 변경 요청.")
data class RenameFriendRequest(
    @field:Schema(description = "내가 정할 친구 이름. 앞뒤 공백을 잘라 1~255자, 아니면 400.", example = "소율이")
    var name: String? = null,
)
