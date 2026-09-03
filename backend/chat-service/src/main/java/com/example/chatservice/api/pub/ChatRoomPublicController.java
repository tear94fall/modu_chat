package com.example.chatservice.api.pub;

import com.example.chatservice.chat.dto.ChatReadCursorDto;
import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto;
import com.example.chatservice.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/** 안드로이드가 게이트웨이를 거쳐 부르는 채팅방 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/chat")
public class ChatRoomPublicController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/{memberId}/rooms")
    public ResponseEntity<List<ChatRoomDto>> chatRoomList(@Valid @PathVariable("memberId") String memberId) {
        return ResponseEntity.ok().body(chatRoomService.searchChatRoomByUserId(memberId));
    }

    @GetMapping("/{roomId}/room")
    public ResponseEntity<ChatRoomDto> getChatRoomInfo(@Valid @PathVariable("roomId") String roomId) {
        return ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId));
    }

    @PostMapping("/chat/room")
    public ResponseEntity<ChatRoomDto> createChatRoom(@Valid @RequestBody List<Long> ids) {
        return ResponseEntity.ok().body(chatRoomService.createChatRoom(ids));
    }

    /**
     * 경로가 /{roomId}/member/{userId} 로 바뀌었다. 예전 /{roomId}/{userId} 는
     * DELETE /{roomId}/{chatId}(메시지 삭제)와 패턴이 같아 요청 시점에 Ambiguous 가 났다.
     */
    @DeleteMapping("/{roomId}/member/{userId}")
    public ResponseEntity<ChatRoomDto> removeChatRoomMember(@Valid @PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(chatRoomService.exitChatRoomMember(roomId, userId));
    }

    @PostMapping("/{roomId}/room")
    public ResponseEntity<ChatRoomDto> updateChatRoom(@Valid @PathVariable("roomId") String roomId, @RequestBody ChatRoomDto requestChatRoomDto) {
        return ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto));
    }

    @PostMapping("/{roomId}/member")
    public ResponseEntity<ChatRoomDto> addMemberChatRoom(@Valid @PathVariable String roomId, @RequestBody List<String> userIds) {
        return ResponseEntity.ok().body(chatRoomService.addMemberChatRoom(roomId, userIds));
    }

    @PostMapping("/room/{userId}")
    public ResponseEntity<List<ChatRoomDto>> getOneOnOneChatRoom(@PathVariable("userId") String userId, @Valid @RequestBody String roomUserId) {
        return ResponseEntity.ok().body(chatRoomService.searchOneOnOneChatRoom(userId, roomUserId));
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<ChatRoomLastReadChatDto>> getUnreadChatRoomChat(@PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(chatRoomService.searchUnreadChatRoom(userId));
    }

    @PostMapping("/read/{roomId}/{userId}")
    public ResponseEntity<Void> updateLastReadChat(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        chatRoomService.updateLastReadChat(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/read/{roomId}")
    public ResponseEntity<List<ChatReadCursorDto>> getReadCursors(@PathVariable("roomId") String roomId) {
        return ResponseEntity.ok().body(chatRoomService.searchReadCursors(roomId));
    }
}
