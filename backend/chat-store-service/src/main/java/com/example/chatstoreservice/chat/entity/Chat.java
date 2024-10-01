package com.example.chatstoreservice.chat.entity;

import com.example.chatstoreservice.chat.dto.ChatDto;
import com.example.chatstoreservice.message.ChatModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import static com.example.chatstoreservice.common.util.TimeUtil.converDateToLocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "chat")
public class Chat {

    @Id
    private String id;
    private Long chatId;
    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;
    private Long chatRoomId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    public void updateChatMessage(String message) {
        this.message = message;
    }

    public Chat(ChatDto chatDto) {
        this.chatType = chatDto.getChatType();
        this.roomId = chatDto.getRoomId();
        this.sender = chatDto.getSender();
        this.message = chatDto.getMessage();
        this.chatTime = chatDto.getChatTime();
    }

    public Chat(ChatModel chatModel) {
        this.chatId = chatModel.getChatId();
        this.chatType = chatModel.getChatType();
        this.roomId = chatModel.getRoomId();
        this.sender = chatModel.getSender();
        this.message = chatModel.getMessage();
        this.chatTime = chatModel.getChatTime();
        this.chatRoomId = chatModel.getChatRoomId();
        this.createdDate = converDateToLocalDateTime(chatModel.getCreatedDate());
        this.updatedDate = converDateToLocalDateTime(chatModel.getUpdatedDate());
    }
}
