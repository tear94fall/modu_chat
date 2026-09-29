package com.example.chatservice.api.pub

import com.example.chatservice.api.common.AuthUserInterceptor.Companion.AUTH_USER_ID_HEADER
import com.example.chatservice.api.dto.ChatDto
import com.example.chatservice.application.usecase.ChatUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

/**
 * 앱이 게이트웨이를 거쳐 부르는 채팅 API. 게이트웨이가 넣어 주는 X-Auth-User-Id 가 없으면 403 이고(AuthUserInterceptor),
 * 방의 대화는 그 방 멤버만 볼 수 있다(ChatAccessGuard).
 */
@Tag(
    name = "채팅 메시지 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요, 게이트웨이가 X-Auth-User-Id 를 넣어 준다. " +
        "헤더가 없으면 403(FORBIDDEN), 방 멤버가 아니면 403(NOT_CHAT_ROOM_MEMBER).",
)
@RestController
@RequestMapping("/api-public/chat")
class ChatPublicController(private val chatUseCase: ChatUseCase) {

    @Operation(
        summary = "메시지 단건 조회",
        description = "메시지 하나를 반응 집계와 함께 돌려준다. 없는 id 면 404(CHAT_NOT_FOUND_ERROR), " +
            "그 메시지가 있는 방의 멤버가 아니면 403.",
    )
    @GetMapping("/{chatId}")
    fun getChat(
        @Parameter(description = "메시지 id(숫자)", example = "1024")
        @Valid @PathVariable("chatId") chatId: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(ChatDto.of(chatUseCase.chat(requesterUserId, chatId.toLong())))

    @Operation(
        summary = "메시지 여러 건 조회",
        description = "id 목록의 메시지를 반응 집계와 함께 돌려준다. 여러 방이 섞여도 된다. " +
            "1:1 방에서 내가 차단한 사람이 보낸 메시지는 뺀다(단체방은 그대로). " +
            "없는 id 와 내가 멤버가 아닌 방의 메시지는 조용히 빠진다.",
    )
    @GetMapping
    fun getChatList(
        @Parameter(description = "메시지 id 목록(쉼표 구분 또는 반복)", example = "1024,1025")
        @Valid @RequestParam("ids") ids: List<String>,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.chats(requesterUserId, ids.map { it.toLong() }).map { ChatDto.of(it) })

    @Operation(
        summary = "방 전체 대화 조회",
        description = "방의 메시지를 전부(페이징 없이) 반응 집계와 함께 돌려준다. 1:1 방이면 내가 차단한 사람의 메시지는 뺀다. " +
            "방 멤버가 아니면 403, 없는 방이면 빈 목록.",
    )
    @GetMapping("/{roomId}/chats")
    fun getChatRoomHistory(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.history(requesterUserId, roomId).map { ChatDto.of(it) })

    @Operation(
        summary = "방 대화 페이지 조회",
        description = "방의 메시지를 대화 시각 최신 순으로 한 쪽씩 돌려준다(page 기본 0, size 기본 20). sort 는 무시한다. 차단 필터는 없다. " +
            "방 멤버가 아니면 403.",
    )
    @GetMapping("/{roomId}/page")
    fun getChatListPaging(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "page(0부터, 기본 0)·size(기본 20). sort 는 쓰지 않는다")
        pageable: Pageable,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.page(requesterUserId, roomId, pageable).map { ChatDto.of(it) })

    @Operation(
        summary = "최근 메시지 조회",
        description = "방에 들어갈 때 쓴다. 최근 메시지 size 개를 최신 순으로 반응 집계와 함께 돌려준다. " +
            "1:1 방이면 내가 차단한 사람의 메시지는 뺀 뒤에 센다. 방 멤버가 아니면 403, size 가 숫자가 아니면 400.",
    )
    @GetMapping("/{roomId}/page/{size}")
    fun getChatListSize(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "가져올 개수(숫자). 상한은 없다", example = "30")
        @Valid @PathVariable("size") size: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.recent(requesterUserId, roomId, size.toLong()).map { ChatDto.of(it) })

    @Operation(
        summary = "이전 메시지 더 보기",
        description = "위로 스크롤할 때 쓴다. chatId 보다 id 가 작은 메시지를 최신 순으로 size 개 돌려준다. " +
            "1:1 방이면 내가 차단한 사람의 메시지는 뺀다. 방 멤버가 아니면 403, chatId·size 가 숫자가 아니면 400.",
    )
    @GetMapping("/{roomId}/{chatId}/{size}")
    fun getPrevChatList(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "기준 메시지 id. 이보다 오래된 것만 온다", example = "1024")
        @Valid @PathVariable("chatId") chatId: String,
        @Parameter(description = "가져올 개수(숫자). 상한은 없다", example = "30")
        @Valid @PathVariable("size") size: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(
            chatUseCase.before(requesterUserId, roomId, chatId.toLong(), size.toLong()).map { ChatDto.of(it) },
        )

    @Operation(
        summary = "방 사진 모아보기 조회",
        description = "방의 이미지 메시지만 최신 순으로 size 개 돌려준다. 1:1 방이면 내가 차단한 사람의 사진은 뺀다. 방 멤버가 아니면 403.",
    )
    @GetMapping("/{roomId}/images/{size}")
    fun getImageChatListSize(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "가져올 개수(숫자). 상한은 없다", example = "50")
        @Valid @PathVariable("size") size: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.images(requesterUserId, roomId, size.toLong()).map { ChatDto.of(it) })

    @Operation(
        summary = "방 메시지 수 조회",
        description = "방의 전체 메시지 수를 문자열로 돌려준다. 없는 방이면 \"0\". 방 멤버가 아니면 403.",
    )
    @GetMapping("/{roomId}/count")
    fun getChatRoomCount(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<String> =
        ResponseEntity.ok().body(chatUseCase.count(requesterUserId, roomId).toString())

    @Operation(
        summary = "방의 메시지 단건 조회",
        description = "방 id 와 메시지 id 가 둘 다 맞는 메시지를 반응 집계와 함께 돌려주고, 없으면 빈 본문(200)이다. " +
            "방 멤버가 아니면 403, chatId 가 숫자가 아니면 400.",
    )
    @GetMapping("/{roomId}/{chatId}")
    fun getChatByChatId(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "메시지 id(숫자)", example = "1024")
        @Valid @PathVariable("chatId") chatId: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(chatUseCase.chatInRoom(requesterUserId, roomId, chatId.toLong())?.let { ChatDto.of(it) })

    @Operation(
        summary = "방 메시지 검색",
        description = "메시지 본문에 message 가 들어 있는 방 메시지를 반응 집계와 함께 돌려준다(부분 일치, 순서 보장 없음). 차단 필터는 없다. " +
            "방 멤버가 아니면 403.",
    )
    @GetMapping("/{roomId}/chat")
    fun getChatByMessage(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "찾을 문구", example = "점심")
        @Valid @RequestParam message: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<List<ChatDto>> =
        ResponseEntity.ok().body(chatUseCase.search(requesterUserId, roomId, message).map { ChatDto.of(it) })

    @Operation(
        summary = "메시지 삭제",
        description = "방에서 내가 보낸 메시지를 지우고 지운 메시지를 돌려준다. 그 메시지의 반응도 같이 지운다. " +
            "보낸 사람이 아니면 403(NOT_CHAT_SENDER), 없는 방·없는 메시지·다른 방의 메시지면 404" +
            "(CHATROOM_NOT_FOUND_ERROR / CHAT_NOT_FOUND_ERROR).",
    )
    @DeleteMapping("/{roomId}/{chatId}")
    fun deleteChat(
        @Parameter(description = "채팅방 id(UUID)", example = ROOM_ID_EXAMPLE)
        @Valid @PathVariable("roomId") roomId: String,
        @Parameter(description = "메시지 id(숫자)", example = "1024")
        @Valid @PathVariable("chatId") chatId: String,
        @Parameter(description = AUTH_USER_ID_DESC, example = AUTH_USER_ID_EXAMPLE)
        @RequestHeader(AUTH_USER_ID_HEADER) requesterUserId: String,
    ): ResponseEntity<ChatDto> =
        ResponseEntity.ok().body(ChatDto.of(chatUseCase.delete(requesterUserId, roomId, chatId.toLong())))

    companion object {
        private const val ROOM_ID_EXAMPLE = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b"
        private const val AUTH_USER_ID_EXAMPLE = "108234567890123456789"
        private const val AUTH_USER_ID_DESC = "게이트웨이가 토큰 subject(요청한 회원의 userId)로 채운다. 없으면 403"
    }
}
