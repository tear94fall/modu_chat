package com.example.chatservice.chat.service

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.entity.Chat
import com.example.chatservice.chat.repository.ChatRepository
import com.example.chatservice.chat.repository.ChatRoomRepository
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.common.exception.ErrorCode
import com.example.chatservice.member.service.BlockedIdsCache
import org.modelmapper.ModelMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val modelMapper: ModelMapper,
    private val blockedIdsCache: BlockedIdsCache,
    private val chatReactionService: ChatReactionService,
) {

    companion object {
        /** 1:1 방의 정의: 방 멤버가 정확히 2명. */
        private const val ONE_ON_ONE_MEMBER_COUNT = 2
    }

    private fun toDto(chat: Chat): ChatDto = modelMapper.map(chat, ChatDto::class.java)

    /**
     * 방 전체 이력. requesterUserId(게이트웨이가 넣는 X-Auth-User-Id)가 있고 1:1 방이면
     * 그 사람이 차단한 발신자의 메시지를 뺀다. 헤더가 없으면(내부 호출) 필터 없음.
     */
    fun searchChatByRoomId(roomId: String, requesterUserId: String?): List<ChatDto> {
        val chatList = chatRepository.findAllByRoomId(roomId, blockedSendersIn(roomId, requesterUserId))
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchChatByRoomIdSize(roomId: String, size: String, requesterUserId: String?): List<ChatDto> {
        val chatList = chatRepository.findByRoomIdSize(roomId, size.toLong(), blockedSendersIn(roomId, requesterUserId))
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchPrevChatByRoomId(roomId: String, chatId: String, size: String, requesterUserId: String?): List<ChatDto> {
        val chatList = chatRepository.findByRoomIdAndChatId(
            roomId, chatId.toLong(), size.toLong(),
            blockedSendersIn(roomId, requesterUserId),
        )
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchChatByRoomIdPaging(roomId: String, pageable: Pageable): List<ChatDto> {
        val chatList = chatRepository.findByRoomIdPaging(roomId, pageable)
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchImageChatByRoomIdSize(roomId: String, size: String, requesterUserId: String?): List<ChatDto> {
        val chatList = chatRepository.findByImageChatSize(roomId, size.toLong(), blockedSendersIn(roomId, requesterUserId))
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchChatByMessage(roomId: String, message: String): List<ChatDto> {
        val chatList = chatRepository.findByMessage(roomId, message)
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun searchChatById(chatId: String): ChatDto {
        val id = chatId.toLong()
        val chat = chatRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId) }
        val dto = toDto(chat)
        dto.reactions = chatReactionService.summaryOf(id)
        return dto
    }

    /**
     * id 목록 조회는 방이 섞여 올 수 있다. 방마다 1:1 인지 따지는 일은 질의가 대신한다
     * (방 멤버 수 = 2 인 방의 차단된 발신자만 제외).
     */
    fun searchChatListById(chatIdList: List<String>, requesterUserId: String?): List<ChatDto> {
        val chatIds = chatIdList.map { it.toLong() }
        val chatList = chatRepository.findAllByIdIn(chatIds, blockedIdsCache.get(requesterUserId))
        return chatReactionService.withReactions(chatList.map(::toDto))
    }

    fun saveChat(chatDto: ChatDto): Long? {
        val chatRoom = chatRoomRepository.findByRoomId(chatDto.roomId!!)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, chatDto.roomId) }

        val chat = Chat(chatDto)
        chat.addChatRoom(chatRoom)
        val saveChat = chatRepository.save(chat)

        chatRoom.addChatting(chat)

        return saveChat.id
    }

    fun deleteChat(roomId: String, chatId: String): ChatDto {
        val chatRoom = chatRoomRepository.findByRoomId(roomId)
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }

        val chat = chatRepository.findById(chatId.toLong())
            .orElseThrow { CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId) }

        chatRoom.removeChat(chat)

        chatRepository.deleteById(chat.id!!)

        return toDto(chat)
    }

    fun searchChatByRoomIdAndChatId(chatId: String, roomId: String): ChatDto? {
        val id = chatId.toLong()
        val chat = chatRepository.findByRoomIdAndChatId(roomId, id) ?: return null
        return toDto(chat)
    }

    fun searchChatCount(roomId: String): String = chatRepository.countByRoomId(roomId).toString()

    /**
     * 이 요청에서 뺄 발신자들. 단체방이면 빈 집합이라 질의가 그대로 돌아간다.
     * 방을 먼저 보는 이유: 단체방에는 필터가 없으므로 member-service 를 부를 필요도 없다.
     */
    private fun blockedSendersIn(roomId: String, requesterUserId: String?): Set<String> {
        if (requesterUserId.isNullOrBlank()) {
            return emptySet()
        }
        if (!isOneOnOneRoom(roomId)) {
            return emptySet()
        }
        return blockedIdsCache.get(requesterUserId)
    }

    private fun isOneOnOneRoom(roomId: String): Boolean =
        chatRoomRepository.findByRoomId(roomId)
            .map { room -> room.chatRoomMemberList.size == ONE_ON_ONE_MEMBER_COUNT }
            .orElse(false)

    /** 백오피스 방 메시지 목록. 최신 순 페이징. */
    @Transactional(readOnly = true)
    fun searchChatsForAdmin(roomId: String, pageable: Pageable): Page<ChatDto> =
        chatRepository.findByRoomId(roomId, pageable).map(::toDto)
}
