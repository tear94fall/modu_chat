package com.example.chatservice.api.internal

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.chat.dto.ReactionRequestDto
import com.example.chatservice.chat.dto.ReactionResultDto
import com.example.chatservice.chat.service.ChatReactionService
import com.example.chatservice.chat.service.ChatRoomService
import com.example.chatservice.chat.service.ChatService
import com.example.chatservice.common.exception.CustomException
import com.example.chatservice.common.exception.ErrorCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** ws-service, chat-store-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal/chat")
class ChatInternalController(
    private val chatService: ChatService,
    private val chatRoomService: ChatRoomService,
    private val chatReactionService: ChatReactionService,
) {

    /** 회원 탈퇴(member-service). 이 회원이 든 모든 방에서 나가고, 비게 된 방은 지운다. 들어 있던 방 PK 목록을 돌려준다. */
    @DeleteMapping("/member/{memberId}/rooms")
    fun exitAllChatRooms(@PathVariable("memberId") memberId: Long): ResponseEntity<List<Long?>> =
        ResponseEntity.ok().body(chatRoomService.exitAllChatRooms(memberId))

    @PostMapping
    fun createChat(@Valid @RequestBody chatDto: ChatDto): ResponseEntity<Long> =
        ResponseEntity.ok().body(chatService.saveChat(chatDto))

    @GetMapping("/{chatId}")
    fun getChat(@Valid @PathVariable("chatId") chatId: String): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatService.searchChatById(chatId))

    @GetMapping("/{roomId}/room")
    fun getChatRoomInfo(@Valid @PathVariable("roomId") roomId: String): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId))

    @GetMapping("/{roomId}/member/{userId}")
    fun checkValidChatRoomMember(
        @Valid @PathVariable("roomId") roomId: String,
        @PathVariable("userId") userId: String,
    ): ResponseEntity<String> {
        val chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId)
        if (chatRoomDto.checkChatRoomMember(userId)) {
            throw CustomException(ErrorCode.INVALID_CHAT_ROOM_MEMBER, roomId)
        }
        return ResponseEntity.ok().body(userId)
    }

    @PostMapping("/{roomId}/room")
    fun updateChatRoom(
        @Valid @PathVariable("roomId") roomId: String,
        @RequestBody requestChatRoomDto: ChatRoomDto,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto))

    /** ws-service 가 REACTION 프레임을 받았을 때. 같은 이모지면 취소, 다른 이모지면 교체. 내 메시지면 400. */
    @PostMapping("/reaction/{roomId}/{chatId}/{userId}")
    fun react(
        @PathVariable("roomId") roomId: String,
        @PathVariable("chatId") chatId: Long,
        @PathVariable("userId") userId: String,
        @RequestBody request: ReactionRequestDto,
    ): ResponseEntity<ReactionResultDto> =
        ResponseEntity.ok().body(chatReactionService.react(roomId, chatId, userId, request.emoji))

    @PostMapping("/read/{roomId}/{userId}")
    fun updateLastReadChat(@PathVariable("roomId") roomId: String, @PathVariable("userId") userId: String): ResponseEntity<Void> {
        chatRoomService.updateLastReadChat(roomId, userId)
        return ResponseEntity.noContent().build()
    }
}
