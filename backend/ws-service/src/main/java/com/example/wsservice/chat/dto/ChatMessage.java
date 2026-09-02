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
    /** type=READ 일 때만 채워진다. 읽은 사람의 userId. */
    private String userId;
}

