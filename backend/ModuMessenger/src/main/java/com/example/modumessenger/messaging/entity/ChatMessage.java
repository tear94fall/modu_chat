package com.example.modumessenger.messaging.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
    private SubscribeType type;
    private String roomId;
    private String chatId;

    @Builder
    public ChatMessage(SubscribeType type, String roomId, String chatId) {
        this.type = type;
        this.roomId = roomId;
        this.chatId = chatId;
    }
}
