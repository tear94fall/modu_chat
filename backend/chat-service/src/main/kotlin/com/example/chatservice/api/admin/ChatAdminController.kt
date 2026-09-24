package com.example.chatservice.api.admin

import com.example.chatservice.api.admin.dto.AdminChatRoomDetailDto
import com.example.chatservice.api.admin.dto.AdminChatRoomSummaryDto
import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.chat.repository.ChatRoomSort
import com.example.chatservice.chat.service.ChatRoomService
import com.example.chatservice.chat.service.ChatService
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
    @GetMapping("/rooms")
    fun rooms(
        @RequestParam(value = "sort", defaultValue = "createdDate,desc") sort: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AdminChatRoomSummaryDto>> {
        val roomSort = ChatRoomSort.parse(sort)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        return ResponseEntity.ok(
            chatRoomService.searchChatRoomsForAdmin(roomSort, PageRequest.of(max(page, 0), min(max(size, 1), 100))),
        )
    }

    @GetMapping("/rooms/{roomId}")
    fun room(@PathVariable("roomId") roomId: String): ResponseEntity<AdminChatRoomDetailDto> =
        ResponseEntity.ok(chatRoomService.searchChatRoomForAdmin(roomId))

    @GetMapping("/rooms/{roomId}/chats")
    fun chats(
        @PathVariable("roomId") roomId: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "50") size: Int,
    ): ResponseEntity<Page<ChatDto>> =
        ResponseEntity.ok(chatService.searchChatsForAdmin(roomId, PageRequest.of(max(page, 0), min(max(size, 1), 100), NEWEST_FIRST)))
}
