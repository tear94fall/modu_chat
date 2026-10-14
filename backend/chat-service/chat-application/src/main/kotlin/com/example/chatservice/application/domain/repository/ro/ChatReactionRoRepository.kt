package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.RoRepository
import com.example.chatservice.application.domain.entity.ChatReaction

interface ChatReactionRoRepository : RoRepository<ChatReaction, Long> {

    /** 이력 한 페이지의 반응을 한 번에 읽는다. 정렬은 남긴 순서(id). */
    fun findAllByChatIdInOrderByIdAsc(chatIds: Collection<Long>): List<ChatReaction>
}
