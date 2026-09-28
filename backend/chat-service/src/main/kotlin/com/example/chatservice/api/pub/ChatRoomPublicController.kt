package com.example.chatservice.api.pub

import com.example.chatservice.chat.dto.ChatReadCursorDto
import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto
import com.example.chatservice.chat.service.ChatRoomService
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 채팅방 API. */
@Tag(
    name = "채팅방 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요.",
)
@RestController
@RequestMapping("/api-public/chat")
class ChatRoomPublicController(private val chatRoomService: ChatRoomService) {

    companion object {
        /** 게이트웨이가 JWT subject(= 회원의 userId, 구글 sub)를 넣어 주는 헤더. */
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
        private const val ROOM_ID_EXAMPLE = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b"
    }

    @Operation(
        summary = "내 채팅방 목록 조회",
        description = "회원이 든 채팅방을 멤버 정보(member-service 에서 조회)와 함께 돌려준다. 든 방이 없으면 빈 목록.",
    )
    @GetMapping("/{memberId}/rooms")
    fun chatRoomList(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @Valid @PathVariable("memberId") memberId: String,
    ): ResponseEntity<List<ChatRoomDto>> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByUserId(memberId))

    @Operation(
        summary = "채팅방 정보 조회",
        description = "방 정보와 멤버 목록(member-service 에서 조회)을 돌려준다. 없는 방이면 500(CHATROOM_NOT_FOUND_ERROR).",
    )
    @GetMapping("/{roomId}/room")
    fun getChatRoomInfo(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId))

    @Operation(
        summary = "채팅방 만들기",
        description = "본문의 회원 id(숫자 PK) 목록으로 방을 만든다(1:1·단체 공통, 중복 id 는 하나로). " +
            "멤버 구성이 정확히 같은 방이 이미 있으면 새로 만들지 않고 그 방을 돌려준다. " +
            "새 방은 이름 \"새로운 채팅방\"으로 만들고 커밋 뒤 방 생성 이벤트(Kafka)를 보낸다. 찾은 회원이 없으면 500(USERID_NOT_FOUND_ERROR).",
    )
    @PostMapping("/chat/room")
    fun createChatRoom(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "방에 넣을 회원 id(숫자 PK) 목록. 나 자신도 포함한다")
        @Valid @RequestBody ids: List<Long>,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.createChatRoom(ids))

    /**
     * 경로가 /{roomId}/member/{userId} 로 바뀌었다. 예전 /{roomId}/{userId} 는
     * DELETE /{roomId}/{chatId}(메시지 삭제)와 패턴이 같아 요청 시점에 Ambiguous 가 났다.
     */
    @Operation(
        summary = "채팅방 나가기",
        description = "member-service 에 알려 회원의 방 목록에서 이 방을 뺀다. 갱신된 방 정보를 돌려준다. " +
            "주의: chat-service 쪽 방 멤버 행은 지우지 않는다(없으면 오히려 추가한다). 없는 방·없는 회원이면 500.",
    )
    @DeleteMapping("/{roomId}/member/{userId}")
    fun removeChatRoomMember(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "나가는 회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.exitChatRoomMember(roomId, userId))

    @Operation(
        summary = "채팅방 정보 수정",
        description = "방 이름·방 이미지·마지막 메시지(내용·id·시각)를 본문 값으로 덮어쓴다. 본문에서 빠진 필드는 null 로 바뀐다. " +
            "없는 방이면 500(CHATROOM_NOT_FOUND_ERROR).",
    )
    @PostMapping("/{roomId}/room")
    fun updateChatRoom(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @RequestBody requestChatRoomDto: ChatRoomDto,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto))

    @Operation(
        summary = "채팅방에 초대",
        description = "본문의 userId 목록을 방에 초대한다. member-service 에 초대를 알리고, 실제로 초대된 회원 중 아직 방에 없는 사람만 멤버로 추가한다. " +
            "찾은 회원이 없거나 없는 방이면 500.",
    )
    @PostMapping("/{roomId}/member")
    fun addMemberChatRoom(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable roomId: String,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "초대할 회원 userId(모두 계정 subject) 목록")
        @RequestBody userIds: List<String>,
    ): ResponseEntity<ChatRoomDto> =
        ResponseEntity.ok().body(chatRoomService.addMemberChatRoom(roomId, userIds))

    @Operation(
        summary = "1:1 채팅방 찾기",
        description = "나(userId)와 상대(본문) 둘만 있는 방을 찾아 돌려준다. 없으면 빈 목록. 회원 조회는 member-service 를 거친다.",
    )
    @PostMapping("/room/{userId}")
    fun getOneOnOneChatRoom(
        @Parameter(description = "내 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "상대 userId. JSON 이 아니라 문자열 그대로 보낸다")
        @Valid @RequestBody roomUserId: String,
    ): ResponseEntity<List<ChatRoomDto>> =
        ResponseEntity.ok().body(chatRoomService.searchOneOnOneChatRoom(userId, roomUserId))

    /**
     * 방별 안 읽은 개수. {userId} 는 이름과 달리 member id(숫자 PK)다 — 안드로이드가
     * myMemberId 를 그대로 넣는다. 차단 조회는 userId(구글 sub) 기준이라
     * 서비스가 member id 를 한 번 변환하고, 변환에 실패하면 게이트웨이가 넣어 준
     * X-Auth-User-Id 를 쓴다.
     */
    @Operation(
        summary = "방별 안 읽은 개수 조회",
        description = "내가 든 방마다 마지막 메시지 id, 내가 마지막으로 읽은 id, 안 읽은 개수를 돌려준다. " +
            "마지막 메시지를 내가 보냈으면 0 이다. 1:1 방에서는 내가 차단한 사람의 메시지를 세지 않는다.",
    )
    @GetMapping("/unread/{userId}")
    fun getUnreadChatRoomChat(
        @Parameter(description = "이름과 달리 회원 id(숫자 PK)", example = "11")
        @PathVariable("userId") userId: String,
        @Parameter(
            description = "게이트웨이가 토큰 subject 로 채운다. member-service 로 userId 를 못 구하면 차단 필터에 대신 쓴다",
            example = "108234567890123456789",
        )
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) requesterUserId: String?,
    ): ResponseEntity<List<ChatRoomLastReadChatDto>> =
        ResponseEntity.ok().body(chatRoomService.searchUnreadChatRoom(userId, requesterUserId))

    @Operation(
        summary = "방 읽음 처리",
        description = "내 읽음 커서를 방의 마지막 메시지로 옮겨 안 읽음 배지를 지운다. 204 를 돌려준다. " +
            "회원 id(숫자)로 먼저 찾고 없으면 userId 로 본다. 없는 방이거나 방 멤버가 아니면 500.",
    )
    @PostMapping("/read/{roomId}/{userId}")
    fun updateLastReadChat(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @PathVariable("roomId") roomId: String,
        @Parameter(description = "회원 id(숫자 PK) 또는 userId", example = "11")
        @PathVariable("userId") userId: String,
    ): ResponseEntity<Void> {
        chatRoomService.updateLastReadChat(roomId, userId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "방 멤버 읽음 위치 조회",
        description = "방 멤버마다 userId 와 마지막으로 읽은 메시지 id 를 돌려준다(말풍선 옆 안 읽은 사람 수 계산용). " +
            "member-service 가 응답하지 않으면 빈 목록. 없는 방이면 500.",
    )
    @GetMapping("/read/{roomId}")
    fun getReadCursors(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @PathVariable("roomId") roomId: String,
    ): ResponseEntity<List<ChatReadCursorDto>> =
        ResponseEntity.ok().body(chatRoomService.searchReadCursors(roomId))
}
