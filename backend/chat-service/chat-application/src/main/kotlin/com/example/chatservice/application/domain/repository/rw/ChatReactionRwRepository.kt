package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.config.RwRepository
import com.example.chatservice.application.domain.entity.ChatReaction
import java.util.Optional

interface ChatReactionRwRepository : RwRepository<ChatReaction, Long> {

    fun findByChatIdAndUserId(chatId: Long, userId: String): Optional<ChatReaction>

    /** 메시지들의 반응을 한 번에 읽는다. 정렬은 남긴 순서(id). */
    fun findAllByChatIdInOrderByIdAsc(chatIds: Collection<Long>): List<ChatReaction>

    fun deleteAllByChatId(chatId: Long)

    fun deleteAllByRoomId(roomId: String)
}
