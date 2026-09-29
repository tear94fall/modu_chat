package com.example.chatservice.application.usecase.result

import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.entity.ChatReaction
import com.example.chatservice.application.domain.entity.ReactionEmoji

/** 메시지 하나. [reactions] 가 null 이면 반응을 읽지 않은 것이다(삭제 응답, 백오피스 목록). */
data class ChatResult(
    val id: Long?,
    val chatType: Int,
    val roomId: String?,
    val sender: String?,
    val message: String?,
    val chatTime: String?,
    val reactions: List<ReactionSummary>? = null,
) {
    companion object {
        fun of(chat: Chat, reactions: List<ReactionSummary>? = null) = ChatResult(
            id = chat.id,
            chatType = chat.chatType,
            roomId = chat.roomId,
            sender = chat.sender,
            message = chat.message,
            chatTime = chat.chatTime,
            reactions = reactions,
        )

        /** 메시지들에 반응 집계를 붙인다. 반응이 없는 메시지는 빈 목록이다. */
        fun withReactions(chats: List<Chat>, reactions: List<ChatReaction>): List<ChatResult> {
            val byChat = reactions.groupBy { it.chatId }
            return chats.map { of(it, ReactionSummary.summarize(byChat[it.id])) }
        }
    }
}

/** 메시지 하나의 이모지별 집계. userIds 로 "내가 남겼는지" 와 "누가 남겼는지" 를 앱이 안다. */
data class ReactionSummary(
    val emoji: String?,
    val count: Int,
    val userIds: List<String>?,
) {
    companion object {
        /** 한 메시지의 반응 줄들을 이모지별로 묶는다. 처음 남겨진 이모지가 앞에 온다. */
        fun summarize(reactions: List<ChatReaction>?): List<ReactionSummary> {
            if (reactions.isNullOrEmpty()) return emptyList()
            val byEmoji = LinkedHashMap<ReactionEmoji, MutableList<String>>()
            for (r in reactions) {
                byEmoji.getOrPut(r.emoji!!) { ArrayList() }.add(r.userId!!)
            }
            return byEmoji.map { (emoji, userIds) -> ReactionSummary(emoji.name, userIds.size, userIds) }
        }
    }
}

/** 반응 토글 결과. ws-service 가 브로드캐스트와 푸시를 만드는 재료다. */
data class ReactionResult(
    val chatId: Long?,
    val roomId: String?,
    /** 메시지 작성자. 푸시 대상. */
    val authorUserId: String?,
    /** true 면 반응이 남겨졌다(생성·교체), false 면 취소됐다. */
    val added: Boolean,
    /** 남겨진 이모지 키. 취소면 null. */
    val emoji: String?,
    val reactions: List<ReactionSummary>?,
)
