package com.example.chatservice.chat.service

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.dto.ReactionResultDto
import com.example.chatservice.chat.dto.ReactionSummaryDto
import com.example.chatservice.chat.entity.ChatReaction
import com.example.chatservice.chat.entity.ReactionEmoji
import com.example.chatservice.chat.repository.ChatReactionRepository
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.common.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 메시지 반응. 한 사람은 한 메시지에 이모지 하나만: 같은 이모지를 다시 보내면 취소, 다른 이모지면 교체.
 * 내 메시지에는 남길 수 없다.
 */
@Service
@Transactional
class ChatReactionService(
    private val chatRepository: ChatRepository,
    private val chatReactionRepository: ChatReactionRepository,
) {

    fun react(roomId: String, chatId: Long, userId: String, rawEmoji: String?): ReactionResultDto {
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

        return ReactionResultDto(
            chatId = chatId,
            roomId = roomId,
            authorUserId = chat.sender,
            added = added,
            emoji = if (added) emoji.name else null,
            reactions = summaryOf(chatId),
        )
    }

    @Transactional(readOnly = true)
    fun summaryOf(chatId: Long): List<ReactionSummaryDto> =
        ReactionSummaryDto.summarize(chatReactionRepository.findAllByChatIdInOrderByIdAsc(listOf(chatId)))

    /** 이력 한 페이지의 DTO 들에 반응 집계를 채운다. 한 번의 IN 질의로 끝낸다. */
    @Transactional(readOnly = true)
    fun withReactions(chats: List<ChatDto>): List<ChatDto> {
        if (chats.isEmpty()) return chats
        val ids = chats.mapNotNull { it.id }
        val byChat = HashMap<Long, MutableList<ChatReaction>>()
        if (ids.isNotEmpty()) {
            for (r in chatReactionRepository.findAllByChatIdInOrderByIdAsc(ids)) {
                byChat.getOrPut(r.chatId!!) { ArrayList() }.add(r)
            }
        }
        chats.forEach { c -> c.reactions = ReactionSummaryDto.summarize(byChat[c.id] ?: emptyList()) }
        return chats
    }

    /** 메시지가 지워지면 반응도 같이 지운다. */
    fun deleteAllOf(chatId: Long) {
        chatReactionRepository.deleteAllByChatId(chatId)
    }
}
