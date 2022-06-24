package com.example.modumessenger.chat.controller;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
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
}
