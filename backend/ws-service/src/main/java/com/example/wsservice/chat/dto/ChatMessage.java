package com.example.wsservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
    private SubscribeType type;
    private String roomId;
    private String chatId;
}

