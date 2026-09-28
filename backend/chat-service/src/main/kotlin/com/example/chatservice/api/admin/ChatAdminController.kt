package com.example.chatservice.api.admin

import com.example.chatservice.api.admin.dto.AdminChatRoomDetailDto
import com.example.chatservice.api.admin.dto.AdminChatRoomSummaryDto
import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.repository.ChatRoomSort
import com.example.chatservice.chat.service.ChatRoomService
import com.example.chatservice.chat.service.ChatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import kotlin.math.max
import kotlin.math.min

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Tag(
    name = "채팅방 관리 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin/chat")
class ChatAdminController(
    private val chatRoomService: ChatRoomService,
    private val chatService: ChatService,
) {

    companion object {
        /** 방 안의 대화 목록은 최신 순. 같은 시각이면 id 내림차순. */
        private val NEWEST_FIRST: Sort = Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"))
    }

    /**
     * 백오피스 방 목록. sort 는 [ChatRoomSort] 허용 목록
     * (roomName | memberCount | lastChatMsg | lastChatTime | createdDate 에 ,asc 또는 ,desc. 기본은 createdDate,desc)
     * 만 받고 모르는 값이면 400 이다. 조용히 기본 정렬로 되돌리면 화면은 정렬된 것처럼 보이는데 값이 다르다.
     * 정렬 자체는 서비스가 붙인다 — 멤버 수는 엔티티 속성이 아니라 Pageable 에 실을 수 없다.
     */
    @Operation(
        summary = "채팅방 목록 조회",
        description = "전체 채팅방을 페이지로 돌려준다. 방마다 멤버 수·마지막 메시지·마지막 대화 시각이 붙는다. " +
            "정렬은 허용 목록만 받고 모르는 값이면 400. 같은 값끼리는 id 순으로 고정된다.",
    )
    @GetMapping("/rooms")
    fun rooms(
        @Parameter(
            description = "정렬. roomName | memberCount | lastChatMsg | lastChatTime | createdDate 뒤에 ,asc 또는 ,desc. 기본 createdDate,desc",
            example = "lastChatTime,desc",
        )
        @RequestParam(value = "sort", defaultValue = "createdDate,desc") sort: String,
        @Parameter(description = "쪽 번호(0부터). 기본 0, 음수는 0", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "쪽 크기. 기본 20, 1~100 으로 잘린다", example = "20")
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AdminChatRoomSummaryDto>> {
        val roomSort = ChatRoomSort.parse(sort)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        return ResponseEntity.ok(
            chatRoomService.searchChatRoomsForAdmin(roomSort, PageRequest.of(max(page, 0), min(max(size, 1), 100))),
        )
    }

    @Operation(
        summary = "채팅방 상세 조회",
        description = "방 정보와 멤버(member-service 에서 조회한 회원 정보), 방 생성 시각을 돌려준다. 없는 방이면 500(CHATROOM_NOT_FOUND_ERROR, 이 서비스엔 예외 처리기가 없다).",
    )
    @GetMapping("/rooms/{roomId}")
    fun room(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @PathVariable("roomId") roomId: String,
    ): ResponseEntity<AdminChatRoomDetailDto> =
        ResponseEntity.ok(chatRoomService.searchChatRoomForAdmin(roomId))

    @Operation(
        summary = "채팅방 대화 목록 조회",
        description = "방의 메시지를 최신 순(생성 시각 내림차순, 같으면 id 내림차순)으로 페이지로 돌려준다. " +
            "차단 필터 없이 전부 보여 준다. 없는 방이면 빈 페이지.",
    )
    @GetMapping("/rooms/{roomId}/chats")
    fun chats(
        @Parameter(description = "채팅방 id(UUID)", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
        @PathVariable("roomId") roomId: String,
        @Parameter(description = "쪽 번호(0부터). 기본 0, 음수는 0", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "쪽 크기. 기본 50, 1~100 으로 잘린다", example = "50")
        @RequestParam(value = "size", defaultValue = "50") size: Int,
    ): ResponseEntity<Page<ChatDto>> =
        ResponseEntity.ok(chatService.searchChatsForAdmin(roomId, PageRequest.of(max(page, 0), min(max(size, 1), 100), NEWEST_FIRST)))
}
