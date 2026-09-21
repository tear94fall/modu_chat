package com.example.wsservice.chat.dto

import java.io.Serializable

/** chat-service 의 집계와 같은 모양. 그대로 앱에 내려간다. */
data class ReactionSummaryDto(
    var emoji: String? = null,
    var count: Int = 0,
    var userIds: List<String>? = null,
) : Serializable
