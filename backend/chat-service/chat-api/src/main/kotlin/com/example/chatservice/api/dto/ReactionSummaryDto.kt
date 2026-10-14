package com.example.chatservice.api.dto

import com.example.chatservice.application.usecase.result.ReactionSummary
import java.io.Serializable

/** 메시지 하나의 이모지별 집계. userIds 로 "내가 남겼는지" 와 "누가 남겼는지" 를 앱이 안다. */
data class ReactionSummaryDto(
    var emoji: String? = null,
    var count: Int = 0,
    var userIds: List<String>? = null,
) : Serializable {
    companion object {
        fun of(summary: ReactionSummary) = ReactionSummaryDto(summary.emoji, summary.count, summary.userIds)
    }
}
