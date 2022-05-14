package com.example.modumessenger.chat.controller;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatService chatService;

    @GetMapping("/chat/{userId}/rooms")
    public ResponseEntity<List<ChatRoomDto>> chatRoomList(@Valid @PathVariable("userId") String userId) {
        List<ChatRoomDto> chatRoomList = chatService.searchChatRoomByUserId(userId);
        return ResponseEntity.ok().body(chatRoomList);
    }

    @GetMapping("/chat/{roomId}/room")
    public ResponseEntity<ChatRoomDto> getChatRoomInfo(@Valid @PathVariable("roomId") String roomId) {
        ChatRoomDto chatRoomDto = chatService.searchChatRoomByRoomId(roomId);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @PostMapping("chat/chat/room")
    public ResponseEntity<ChatRoomDto> createChatRoom(@Valid @RequestBody List<String> userId) {
        ChatRoomDto chatRoomDto = chatService.createChatRoom(userId);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @GetMapping("/chat/{roomId}/chats")
    public ResponseEntity<List<ChatDto>> getChatRoomHistory(@Valid @PathVariable("roomId") String roomId) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomId(roomId);
        return ResponseEntity.ok().body(chatDtoList);
    }
}
