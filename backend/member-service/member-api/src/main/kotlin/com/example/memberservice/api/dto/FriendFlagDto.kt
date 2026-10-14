package com.example.memberservice.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/** 즐겨찾기·숨김·차단 켜기/끄기 요청 본문: `{"on": true}`. */
@Schema(description = "친구 즐겨찾기·숨기기·차단 켜기/끄기 요청.")
data class FriendFlagDto(
    @field:Schema(description = "true 면 켜고 false 면 끈다. 빠지면 false.", example = "true")
    var on: Boolean = false,
)
