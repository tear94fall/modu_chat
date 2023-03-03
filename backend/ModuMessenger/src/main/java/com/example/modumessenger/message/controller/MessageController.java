package com.example.modumessenger.message.controller;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.message.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<ChatDto> send(@RequestBody ChatDto chatDto) throws JsonProcessingException {
        messageService.sendMessage(chatDto);
        return ResponseEntity.ok(chatDto);
    }
}
