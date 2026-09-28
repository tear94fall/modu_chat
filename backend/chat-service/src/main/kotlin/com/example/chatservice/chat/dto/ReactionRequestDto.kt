package com.example.chatservice.chat.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "메시지 반응 요청")
data class ReactionRequestDto(
    @field:Schema(
        description = "이모지 키. LIKE | HEART | LAUGH | WOW | SAD | PRAY 중 하나(대소문자·앞뒤 공백 무시). 그 밖의 값이면 실패",
        example = "HEART",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    var emoji: String? = null,
)
