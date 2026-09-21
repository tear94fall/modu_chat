package com.example.chatservice.chat.dto

import com.example.chatservice.chat.entity.ChatReaction
import com.example.chatservice.chat.entity.ReactionEmoji
import java.io.Serializable

/** 메시지 하나의 이모지별 집계. userIds 로 "내가 남겼는지" 와 "누가 남겼는지" 를 앱이 안다. */
data class ReactionSummaryDto(
    var emoji: String? = null,
    var count: Int = 0,
    var userIds: List<String>? = null,
) : Serializable {

    companion object {
        /** 한 메시지의 반응 줄들을 이모지별로 묶는다. 처음 남겨진 이모지가 앞에 온다. */
        @JvmStatic
        fun summarize(reactions: List<ChatReaction>?): List<ReactionSummaryDto> {
            if (reactions.isNullOrEmpty()) return emptyList()
            val byEmoji = LinkedHashMap<ReactionEmoji, MutableList<String>>()
            for (r in reactions) {
                byEmoji.getOrPut(r.emoji!!) { ArrayList() }.add(r.userId!!)
            }
            return byEmoji.map { (emoji, userIds) -> ReactionSummaryDto(emoji.name, userIds.size, userIds) }
        }
    }
}
