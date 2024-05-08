package com.example.chatstoreservice.chat.entity;

import com.example.chatstoreservice.chat.dto.ChatDto;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "chat")
public class Chat {

    @Id
    private String id;
    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;

    public Chat(ChatDto chatDto) {
        this.chatType = chatDto.getChatType();
        this.roomId = chatDto.getRoomId();
        this.sender = chatDto.getSender();
        this.message = chatDto.getMessage();
        this.chatTime = chatDto.getChatTime();
    }
}
