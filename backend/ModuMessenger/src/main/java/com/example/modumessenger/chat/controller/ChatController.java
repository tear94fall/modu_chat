package com.example.modumessenger.chat.controller;

import com.example.modumessenger.chat.entity.Chat;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations sendingOperations;

    @Scheduled(fixedRate = 5000)
    @MessageMapping("/comm/chat")
    public void message(Chat chat) {
        if (Chat.ChatType.ENTER.equals(chat.getChatType())) {
            chat.setMessage(chat.getSender() + "이 입장했습니다.");
        }
        sendingOperations.convertAndSend("/sub/comm/room/" + chat.getRoomId(), chat);
    }
}
