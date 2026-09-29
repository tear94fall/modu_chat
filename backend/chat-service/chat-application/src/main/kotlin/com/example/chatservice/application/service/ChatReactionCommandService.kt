package com.example.chatservice.application.service

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.config.RwJpaConfig
import com.example.chatservice.application.domain.entity.ChatReaction
import com.example.chatservice.application.domain.entity.ReactionEmoji
import com.example.chatservice.application.domain.repository.rw.ChatReactionRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRwRepository
import com.example.chatservice.application.usecase.result.ReactionResult
import com.example.chatservice.application.usecase.result.ReactionSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 메시지 반응. 한 사람은 한 메시지에 이모지 하나만: 같은 이모지를 다시 보내면 취소, 다른 이모지면 교체.
 * 내 메시지에는 남길 수 없다. 이미 남긴 반응이 있는지는 master 로 본다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class ChatReactionCommandService(
    private val chatRepository: ChatRwRepository,
    private val chatReactionRepository: ChatReactionRwRepository,
) {

    fun react(roomId: String, chatId: Long, userId: String, rawEmoji: String?): ReactionResult {
        val emoji = ReactionEmoji.of(rawEmoji) ?: throw CustomException(ErrorCode.INVALID_REACTION, rawEmoji)
        val chat = chatRepository.findById(chatId)
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId) }
        if (roomId != chat.roomId) {
            throw CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId)
        }
        if (userId == chat.sender) {
            throw CustomException(ErrorCode.CANNOT_REACT_OWN_CHAT, chatId)
        }

        val existing = chatReactionRepository.findByChatIdAndUserId(chatId, userId).orElse(null)
        val added: Boolean
        if (existing == null) {
            chatReactionRepository.save(ChatReaction(chatId, roomId, userId, emoji))
            added = true
        } else if (existing.emoji == emoji) {
            chatReactionRepository.delete(existing)
            added = false
        } else {
            existing.change(emoji)
            added = true
        }
        chatReactionRepository.flush()

        return ReactionResult(
            chatId = chatId,
            roomId = roomId,
            authorUserId = chat.sender,
            added = added,
            emoji = if (added) emoji.name else null,
            reactions = ReactionSummary.summarize(chatReactionRepository.findAllByChatIdInOrderByIdAsc(listOf(chatId))),
        )
    }
}
