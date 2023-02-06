package com.example.modumessenger.message.controller;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<ChatDto> send(ChatDto chatDto) {
        messageService.sendMessage(chatDto);
        return ResponseEntity.ok(chatDto);
    }
}
