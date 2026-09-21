package com.example.chatservice.api.pub

import com.example.chatservice.chat.dto.ChatReadCursorDto
import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto
import com.example.chatservice.chat.service.ChatRoomService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 채팅방 API. */
@RestController
@RequestMapping("/api-public/chat")
class ChatRoomPublicController(private val chatRoomService: ChatRoomService) {

    companion object {
        /** 게이트웨이가 JWT subject(= 회원의 userId, 구글 sub)를 넣어 주는 헤더. */
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
    }

    @GetMapping("/{memberId}/rooms")
    fun chatRoomList(@Valid @PathVariable("memberId") memberId: String): ResponseEntity<List<ChatRoomDto>> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByUserId(memberId))

    @GetMapping("/{roomId}/room")
    fun getChatRoomInfo(@Valid @PathVariable("roomId") roomId: String): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId))

    @PostMapping("/chat/room")
    fun createChatRoom(@Valid @RequestBody ids: List<Long>): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.createChatRoom(ids))

    /**
     * 경로가 /{roomId}/member/{userId} 로 바뀌었다. 예전 /{roomId}/{userId} 는
     * DELETE /{roomId}/{chatId}(메시지 삭제)와 패턴이 같아 요청 시점에 Ambiguous 가 났다.
     */
    @DeleteMapping("/{roomId}/member/{userId}")
    fun removeChatRoomMember(
        @Valid @PathVariable("roomId") roomId: String,
        @PathVariable("userId") userId: String,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.exitChatRoomMember(roomId, userId))

    @PostMapping("/{roomId}/room")
    fun updateChatRoom(
        @Valid @PathVariable("roomId") roomId: String,
        @RequestBody requestChatRoomDto: ChatRoomDto,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto))

    @PostMapping("/{roomId}/member")
    fun addMemberChatRoom(
        @Valid @PathVariable roomId: String,
        @RequestBody userIds: List<String>,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.addMemberChatRoom(roomId, userIds))

    @PostMapping("/room/{userId}")
    fun getOneOnOneChatRoom(
        @PathVariable("userId") userId: String,
        @Valid @RequestBody roomUserId: String,
    ): ResponseEntity<List<ChatRoomDto>> =
        ResponseEntity.ok().body(chatRoomService.searchOneOnOneChatRoom(userId, roomUserId))

    /**
     * 방별 안 읽은 개수. {userId} 는 이름과 달리 member id(숫자 PK)다 — 안드로이드가
     * myMemberId 를 그대로 넣는다. 차단 조회는 userId(구글 sub) 기준이라
     * 서비스가 member id 를 한 번 변환하고, 변환에 실패하면 게이트웨이가 넣어 준
     * X-Auth-User-Id 를 쓴다.
     */
    @GetMapping("/unread/{userId}")
    fun getUnreadChatRoomChat(
        @PathVariable("userId") userId: String,
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatRoomLastReadChatDto>> =
        ResponseEntity.ok().body(chatRoomService.searchUnreadChatRoom(userId, requesterUserId))

    @PostMapping("/read/{roomId}/{userId}")
    fun updateLastReadChat(@PathVariable("roomId") roomId: String, @PathVariable("userId") userId: String): ResponseEntity<Void> {
        chatRoomService.updateLastReadChat(roomId, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/read/{roomId}")
    fun getReadCursors(@PathVariable("roomId") roomId: String): ResponseEntity<List<ChatReadCursorDto>> =
        ResponseEntity.ok().body(chatRoomService.searchReadCursors(roomId))
}
