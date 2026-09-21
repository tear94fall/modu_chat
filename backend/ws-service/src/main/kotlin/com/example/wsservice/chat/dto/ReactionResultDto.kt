package com.example.wsservice.chat.dto

/** chat-service 반응 토글 응답. */
data class ReactionResultDto(
    var chatId: Long? = null,
    var roomId: String? = null,
    var authorUserId: String? = null,
    var added: Boolean = false,
    var emoji: String? = null,
    var reactions: List<ReactionSummaryDto>? = null,
)
