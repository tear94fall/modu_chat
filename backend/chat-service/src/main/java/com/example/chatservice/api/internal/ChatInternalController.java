package com.example.chatservice.api.internal;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.chat.service.ChatService;
import com.example.chatservice.chat.service.ChatRoomService;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** ws-service, chat-store-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/chat")
public class ChatInternalController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<Long> createChat(@Valid @RequestBody ChatDto chatDto) {
        return ResponseEntity.ok().body(chatService.saveChat(chatDto));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto> getChat(@Valid @PathVariable("chatId") String chatId) {
        return ResponseEntity.ok().body(chatService.searchChatById(chatId));
    }

    @GetMapping("/{roomId}/room")
    public ResponseEntity<ChatRoomDto> getChatRoomInfo(@Valid @PathVariable("roomId") String roomId) {
        return ResponseEntity.ok().body(chatRoomService.searchChatRoomByRoomId(roomId));
    }

    @GetMapping("/{roomId}/member/{userId}")
    public ResponseEntity<String> checkValidChatRoomMember(@Valid @PathVariable("roomId") String roomId, @PathVariable("userId") String userId) throws CustomException {
        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);
        if (chatRoomDto.checkChatRoomMember(userId))
            throw new CustomException(ErrorCode.INVALID_CHAT_ROOM_MEMBER, roomId);
        return ResponseEntity.ok().body(userId);
    }

    @PostMapping("/{roomId}/room")
    public ResponseEntity<ChatRoomDto> updateChatRoom(@Valid @PathVariable("roomId") String roomId, @RequestBody ChatRoomDto requestChatRoomDto) {
        return ResponseEntity.ok().body(chatRoomService.updateChatRoom(roomId, requestChatRoomDto));
    }

    @PostMapping("/read/{roomId}/{userId}")
    public ResponseEntity<Void> updateLastReadChat(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        chatRoomService.updateLastReadChat(roomId, userId);
        return ResponseEntity.noContent().build();
    }
}
