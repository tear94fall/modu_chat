package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatReaction
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatReactionRepository : JpaRepository<ChatReaction, Long> {

    fun findByChatIdAndUserId(chatId: Long, userId: String): Optional<ChatReaction>

    /** 이력 한 페이지의 반응을 한 번에 읽는다. 정렬은 남긴 순서(id). */
    fun findAllByChatIdInOrderByIdAsc(chatIds: Collection<Long>): List<ChatReaction>

    fun deleteAllByChatId(chatId: Long)
}
