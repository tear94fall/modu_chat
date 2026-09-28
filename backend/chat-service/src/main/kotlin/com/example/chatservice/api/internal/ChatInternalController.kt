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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "채팅 (내부)",
    description = "서비스끼리만 호출(ws-service·chat-store-service·member-service). X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.",
)
@RestController
@RequestMapping("/api-internal/chat")
class ChatInternalController(
    private val chatService: ChatService,
    private val chatRoomService: ChatRoomService,
    private val chatReactionService: ChatReactionService,
) {

    /** 회원 탈퇴(member-service). 이 회원이 든 모든 방에서 나가고, 비게 된 방은 지운다. 들어 있던 방 PK 목록을 돌려준다. */
    @Operation(
        summary = "탈퇴 회원 전체 방 나가기",
        description = "회원 탈퇴 때 member-service 가 부른다. 이 회원의 방 멤버 행을 모두 지우고, 멤버가 없어진 방은 대화째 지운다. " +
            "들어 있던 방의 PK 목록을 돌려준다(방이 없으면 빈 목록). member-service 를 다시 부르지 않는다.",
    )
    @DeleteMapping("/member/{memberId}/rooms")
    fun exitAllChatRooms(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: Long,
    ): ResponseEntity<List<Long?>> =
        ResponseEntity.ok().body(chatRoomService.exitAllChatRooms(memberId))

    @Operation(
        summary = "메시지 저장",
        description = "ws-service·chat-store-service 가 받은 메시지를 DB 에 저장하고 방의 마지막 메시지를 갱신한다. " +
            "새 메시지 id 를 돌려준다. 본문의 roomId 방이 없으면 500(CHATROOM_NOT_FOUND_ERROR).",
    )
    @PostMapping
    fun createChat(@Valid @RequestBody chatDto: ChatDto): ResponseEntity<Long> =
        ResponseEntity.ok().body(chatService.saveChat(chatDto))

    @Operation(
        summary = "메시지 단건 조회",
        description = "메시지 하나를 반응 집계와 함께 돌려준다. 없는 id 면 500(CHAT_NOT_FOUND_ERROR).",
    )
    @GetMapping("/{chatId}")
    fun getChat(
        @Parameter(description = "메시지 id(숫자)", example = "1024")
        @Valid @PathVariable("chatId") chatId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatService.searchChatById(chatId))

    @Operation(
        summary = "채팅방 정보 조회",
        description = "방 정보와 멤버 목록(member-service 에서 조회)을 돌려준다. ws-service 가 방 생성 이벤트를 받은 뒤 부른다. " +
            "없는 방이면 500(CHATROOM_NOT_FOUND_ERROR).",
    )
    @GetMapping("/{roomId}/room")
    fun getChatRoomInfo(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @Valid @PathVariable("roomId") roomId: String,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId))

    @Operation(
        summary = "방 멤버인지 확인",
        description = "userId 가 방 멤버면 그 userId 를 그대로 돌려준다. 멤버가 아니거나 없는 방이면 500(INVALID_CHAT_ROOM_MEMBER / CHATROOM_NOT_FOUND_ERROR).",
    )
    @GetMapping("/{roomId}/member/{userId}")
    fun checkValidChatRoomMember(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<String> {
        val chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId)
        if (chatRoomDto.checkChatRoomMember(userId)) {
            throw CustomException(ErrorCode.INVALID_CHAT_ROOM_MEMBER, roomId)
        }
        return ResponseEntity.ok().body(userId)
    }

    @Operation(
        summary = "채팅방 정보 수정",
        description = "방 이름·방 이미지·마지막 메시지(내용·id·시각)를 본문 값으로 덮어쓴다. 본문에서 빠진 필드는 null 로 바뀐다. " +
            "없는 방이면 500(CHATROOM_NOT_FOUND_ERROR).",
    )
    @PostMapping("/{roomId}/room")
    fun updateChatRoom(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @Valid @PathVariable("roomId") roomId: String,
        @RequestBody requestChatRoomDto: ChatRoomDto,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto))

    /** ws-service 가 REACTION 프레임을 받았을 때. 같은 이모지면 취소, 다른 이모지면 교체. 내 메시지면 400. */
    @Operation(
        summary = "메시지 반응 남기기",
        description = "ws-service 가 REACTION 프레임을 받았을 때 부른다. 한 사람은 한 메시지에 이모지 하나: 처음이면 추가, 같은 이모지면 취소, " +
            "다른 이모지면 교체한다. 결과(added, 남은 반응 집계, 작성자 userId)를 돌려준다. " +
            "모르는 이모지·내 메시지·다른 방 메시지·없는 메시지면 500(INVALID_REACTION / CANNOT_REACT_OWN_CHAT / CHAT_NOT_FOUND_ERROR).",
    )
    @PostMapping("/reaction/{roomId}/{chatId}/{userId}")
    fun react(
        @Parameter(description = "채팅방 id(UUID). 메시지가 이 방 것이어야 한다", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @PathVariable("roomId") roomId: String,
        @Parameter(description = "메시지 id", example = "1024")
        @PathVariable("chatId") chatId: Long,
        @Parameter(description = "반응을 남기는 회원 userId", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
        @RequestBody request: ReactionRequestDto,
    ): ResponseEntity<ReactionResultDto> =
        ResponseEntity.ok().body(chatReactionService.react(roomId, chatId, userId, request.emoji))

    @Operation(
        summary = "마지막 읽은 메시지 갱신",
        description = "ws-service 의 READ 프레임용. 이 회원의 읽음 커서를 방의 마지막 메시지로 옮긴다. userId 는 회원 id(숫자) 또는 userId 문자열 둘 다 받는다. " +
            "204 를 돌려준다. 없는 방이거나 방 멤버가 아니면 500.",
    )
    @PostMapping("/read/{roomId}/{userId}")
    fun updateLastReadChat(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @PathVariable("roomId") roomId: String,
        @Parameter(description = "회원 id(숫자 PK) 또는 userId", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<Void> {
        chatRoomService.updateLastReadChat(roomId, userId)
        return ResponseEntity.noContent().build()
    }
}
