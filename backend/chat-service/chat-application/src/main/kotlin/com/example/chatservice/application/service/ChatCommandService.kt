package com.example.chatservice.application.service

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.config.RwJpaConfig
import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.repository.rw.ChatReactionRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRwRepository
import com.example.chatservice.application.usecase.command.SaveChatCommand
import com.example.chatservice.application.usecase.result.ChatResult
import com.example.chatservice.application.usecase.result.ReactionSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 메시지 쓰기, 그리고 쓰기 직후에 읽히는 조회(master).
 *
 * ws-service 는 메시지를 저장하자마자 id 로 다시 읽어 소켓에 뿌리고, 앱은 새 메시지·반응 알림을 받자마자
 * id 로 조회하거나 방에 들어가며 최근 메시지를 읽는다. replica 로 읽으면 방금 저장한 메시지가 없다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class ChatCommandService(
    private val chatRoomRepository: ChatRoomRwRepository,
    private val chatRepository: ChatRwRepository,
    private val chatReactionRepository: ChatReactionRwRepository,
) {

    // ---------- 읽기(master) ----------

    /** 메시지 하나와 반응 집계. 없으면 404. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findById(chatId: Long): ChatResult {
        val chat = chatRepository.findById(chatId)
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId) }
        return ChatResult.of(chat, summaryOf(chatId))
    }

    /** 방 id 와 메시지 id 가 둘 다 맞는 메시지와 반응 집계. 없으면 null. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findByRoomIdAndChatId(roomId: String, chatId: Long): ChatResult? {
        val chat = chatRepository.findByRoomIdAndChatId(roomId, chatId) ?: return null
        return ChatResult.of(chat, summaryOf(chatId))
    }

    /**
     * id 목록 조회는 방이 섞여 올 수 있다. 방마다 1:1 인지 따지는 일은 질의가 대신한다
     * (방 멤버 수 = 2 인 방의 차단된 발신자만 제외).
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findAllByIds(chatIds: List<Long>, blockedSenders: Set<String>): List<ChatResult> =
        withReactions(chatRepository.findAllByIdIn(chatIds, blockedSenders))

    /** 방의 최근 메시지 size 개(최신 순). 방에 들어갈 때와 재접속 뒤 빈 구간을 메울 때 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findRecent(roomId: String, size: Long, blockedSenders: Set<String>): List<ChatResult> =
        withReactions(chatRepository.findByRoomIdSize(roomId, size, blockedSenders))

    // ---------- 쓰기 ----------

    /** 메시지를 저장하고 방의 마지막 메시지(id·내용)를 갱신한다. 방이 없으면 404. */
    fun save(command: SaveChatCommand): Long? {
        val chatRoom = chatRoomRepository.findByRoomId(command.roomId!!)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, command.roomId) }

        val chat = Chat(command.message, command.roomId, chatRoom, command.sender, command.chatTime, command.chatType)
        val saved = chatRepository.save(chat)

        chatRoom.addChatting(chat)

        return saved.id
    }

    /**
     * 보낸 사람만 자기 메시지를 지울 수 있다. 반응도 같이 지우고, 방의 마지막 메시지 id 는 남은 것 중 가장 최근 것으로 바꾼다.
     * 없는 방·없는 메시지·다른 방의 메시지면 404, 보낸 사람이 아니면 403.
     */
    fun delete(roomId: String, chatId: Long, requesterUserId: String): ChatResult {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }
        val chat = chatRepository.findById(chatId)
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId) }
        if (roomId != chat.roomId) {
            throw CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId)
        }
        if (requesterUserId != chat.sender) {
            throw CustomException(ErrorCode.NOT_CHAT_SENDER, chatId)
        }

        val deleted = ChatResult.of(chat)
        val lastRemaining = chatRepository.findFirstByRoomIdAndIdNotOrderByIdDesc(roomId, chatId).map { it.id }.orElse(null)

        chatReactionRepository.deleteAllByChatId(chatId)
        chatRepository.delete(chat)
        chatRoom.chatDeleted(lastRemaining)

        return deleted
    }

    private fun summaryOf(chatId: Long): List<ReactionSummary> =
        ReactionSummary.summarize(chatReactionRepository.findAllByChatIdInOrderByIdAsc(listOf(chatId)))

    /** 한 번의 IN 질의로 반응을 읽어 붙인다. */
    private fun withReactions(chats: List<Chat>): List<ChatResult> {
        val ids = chats.mapNotNull { it.id }
        val reactions = if (ids.isEmpty()) emptyList() else chatReactionRepository.findAllByChatIdInOrderByIdAsc(ids)
        return ChatResult.withReactions(chats, reactions)
    }
}
