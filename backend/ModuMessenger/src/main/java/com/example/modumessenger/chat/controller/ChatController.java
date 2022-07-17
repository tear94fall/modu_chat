package com.example.modumessenger.chat.controller;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/chat/{roomId}/chats")
    public ResponseEntity<List<ChatDto>> getChatRoomHistory(@Valid @PathVariable("roomId") String roomId) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomId(roomId);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/page")
    public ResponseEntity<List<ChatDto>> getChatListPaging(@Valid @PathVariable("roomId") String roomId, Pageable pageable) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomIdPaging(roomId, pageable);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/{chatId}/chats")
    public ResponseEntity<List<ChatDto>> getPrevChatList(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId, String size) {
        List<ChatDto> chatDtoList = chatService.searchPrevChatByRoomId(roomId, chatId, size);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/count")
    public ResponseEntity<String> getChatRoomCount(@Valid @PathVariable("roomId") String roomId) {
        String count = chatService.searchChatCount(roomId);
        return ResponseEntity.ok().body(count);
    }

    @GetMapping("/chat/{roomId}/{chatId}")
    public ResponseEntity<ChatDto> getChatByChatId(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId) {
        ChatDto chatDto = chatService.searchChatByRoomIdAndChatId(roomId, chatId);
        return ResponseEntity.ok().body(chatDto);
    }

    @GetMapping("/chat/{roomId}/chat")
    public ResponseEntity<List<ChatDto>> getChatByMessage(@Valid @PathVariable("roomId") String roomId, @Valid @RequestParam String message) {
        List<ChatDto> chatDtoList = chatService.searchChatByMessage(roomId, message);
        return ResponseEntity.ok().body(chatDtoList);
    }
}
