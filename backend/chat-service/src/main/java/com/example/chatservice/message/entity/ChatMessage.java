package com.example.chatservice.message.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
    private SubscribeType type;
    private String roomId;
    private String chatId;
}

