package com.example.chatservice.application.service

import com.example.chatservice.application.config.RoJpaConfig
import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.repository.ro.ChatReactionRoRepository
import com.example.chatservice.application.domain.repository.ro.ChatRoRepository
import com.example.chatservice.application.usecase.result.ChatResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 지난 대화 둘러보기(replica): 방 전체 이력, 페이지, 이전 메시지, 사진 모아보기, 검색, 개수, 백오피스 목록.
 * 복제가 따라오기 전의 아주 최근 메시지는 빠질 수 있다 — 최근 메시지는 ChatCommandService 가 master 로 읽는다.
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class ChatQueryService(
    private val chatRepository: ChatRoRepository,
    private val chatReactionRepository: ChatReactionRoRepository,
) {

    fun findAllByRoom(roomId: String, blockedSenders: Set<String>): List<ChatResult> =
        withReactions(chatRepository.findAllByRoomId(roomId, blockedSenders))

    fun findPage(roomId: String, pageable: Pageable): List<ChatResult> =
        withReactions(chatRepository.findByRoomIdPaging(roomId, pageable))

    /** chatId 보다 id 가 작은 메시지 size 개(최신 순). */
    fun findBefore(roomId: String, chatId: Long, size: Long, blockedSenders: Set<String>): List<ChatResult> =
        withReactions(chatRepository.findByRoomIdAndChatId(roomId, chatId, size, blockedSenders))

    fun findImages(roomId: String, size: Long, blockedSenders: Set<String>): List<ChatResult> =
        withReactions(chatRepository.findByImageChatSize(roomId, size, blockedSenders))

    fun search(roomId: String, message: String): List<ChatResult> =
        withReactions(chatRepository.findByMessage(roomId, message))

    fun count(roomId: String): Long = chatRepository.countByRoomId(roomId)

    /** 백오피스 방 메시지 목록. 반응은 읽지 않는다. */
    fun findPageForAdmin(roomId: String, pageable: Pageable): Page<ChatResult> =
        chatRepository.findByRoomId(roomId, pageable).map { ChatResult.of(it) }

    /** 이력 한 페이지의 반응을 한 번의 IN 질의로 읽어 붙인다. */
    private fun withReactions(chats: List<Chat>): List<ChatResult> {
        val ids = chats.mapNotNull { it.id }
        val reactions = if (ids.isEmpty()) emptyList() else chatReactionRepository.findAllByChatIdInOrderByIdAsc(ids)
        return ChatResult.withReactions(chats, reactions)
    }
}
