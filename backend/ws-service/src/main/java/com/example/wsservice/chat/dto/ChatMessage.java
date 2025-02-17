package com.example.wsservice.chat.dto;

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

