package com.example.chatservice.api.pub

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.service.ChatService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 채팅 API. */
@RestController
@RequestMapping("/api-public/chat")
class ChatPublicController(private val chatService: ChatService) {

    companion object {
        /** 게이트웨이가 JWT subject(= 회원의 userId, 구글 sub)를 넣어 주는 헤더. 없으면 필터하지 않는다. */
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
    }

    @GetMapping("/{chatId}")
    fun getChat(@Valid @PathVariable("chatId") chatId: String): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatService.searchChatById(chatId))

    @GetMapping
    fun getChatList(
        @Valid @RequestParam("ids") ids: List<String>,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchChatListById(ids, requesterUserId))

    @GetMapping("/{roomId}/chats")
    fun getChatRoomHistory(
        @Valid @PathVariable("roomId") roomId: String,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchChatByRoomId(roomId, requesterUserId))

    @GetMapping("/{roomId}/page")
    fun getChatListPaging(@Valid @PathVariable("roomId") roomId: String, pageable: Pageable): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchChatByRoomIdPaging(roomId, pageable))

    @GetMapping("/{roomId}/page/{size}")
    fun getChatListSize(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @PathVariable("size") size: String,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchChatByRoomIdSize(roomId, size, requesterUserId))

    @GetMapping("/{roomId}/{chatId}/{size}")
    fun getPrevChatList(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @PathVariable("chatId") chatId: String,
        @Valid @PathVariable("size") size: String,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchPrevChatByRoomId(roomId, chatId, size, requesterUserId))

    @GetMapping("/{roomId}/images/{size}")
    fun getImageChatListSize(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @PathVariable("size") size: String,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchImageChatByRoomIdSize(roomId, size, requesterUserId))

    @GetMapping("/{roomId}/count")
    fun getChatRoomCount(@Valid @PathVariable("roomId") roomId: String): ResponseEntity<String> =
        ResponseEntity.ok().body(chatService.searchChatCount(roomId))

    @GetMapping("/{roomId}/{chatId}")
    fun getChatByChatId(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @PathVariable("chatId") chatId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatService.searchChatByRoomIdAndChatId(roomId, chatId))

    @GetMapping("/{roomId}/chat")
    fun getChatByMessage(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @RequestParam message: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatService.searchChatByMessage(roomId, message))

    @DeleteMapping("/{roomId}/{chatId}")
    fun deleteChat(
        @Valid @PathVariable("roomId") roomId: String,
        @Valid @PathVariable("chatId") chatId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatService.deleteChat(roomId, chatId))
}
