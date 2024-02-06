package com.example.chatservice.chat.entity;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.common.domain.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat extends BaseTimeEntity {
    
    @Id
    @Column(name = "chat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    @ToString.Exclude
    private ChatRoom chatRoom;

    public void addChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public Chat(String msg) { this.message = msg; }

    public Chat(String msg, ChatRoom chatRoom) {
        this.message = msg;
        this.chatRoom = chatRoom;
    }

    public Chat(String msg, String roomId, ChatRoom chatRoom, String sender, String chatTime, int type) {
        this.message = msg;
        this.roomId = roomId;
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.chatTime = chatTime;
        this.chatType = type;
    }

    public Chat(ChatDto chatDto) {
        this.message = chatDto.getMessage();
        this.roomId = chatDto.getRoomId();
        this.sender = chatDto.getSender();
        this.chatTime = chatDto.getChatTime();
        this.chatType = chatDto.getChatType();
    }
}
