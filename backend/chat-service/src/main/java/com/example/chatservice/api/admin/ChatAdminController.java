package com.example.chatservice.api.admin;

import com.example.chatservice.api.admin.dto.AdminChatRoomSummaryDto;
import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.chat.service.ChatRoomService;
import com.example.chatservice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-admin/chat")
public class ChatAdminController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    /** 백오피스 목록은 최신 생성 순. 같은 시각이면 id 내림차순. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"));

    @GetMapping("/rooms")
    public ResponseEntity<Page<AdminChatRoomSummaryDto>> rooms(@RequestParam(value = "page", defaultValue = "0") int page,
                                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(chatRoomService.searchChatRoomsForAdmin(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), NEWEST_FIRST)));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDto> room(@PathVariable("roomId") String roomId) {
        return ResponseEntity.ok(chatRoomService.searchChatRoomByRoomId(roomId));
    }

    @GetMapping("/rooms/{roomId}/chats")
    public ResponseEntity<Page<ChatDto>> chats(@PathVariable("roomId") String roomId,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.searchChatsForAdmin(roomId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), NEWEST_FIRST)));
    }
}
