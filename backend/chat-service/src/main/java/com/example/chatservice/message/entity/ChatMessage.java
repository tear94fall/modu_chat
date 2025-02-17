package com.example.chatservice.message.entity;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private SubscribeType type;
    private String roomId;
    private String chatId;
}

