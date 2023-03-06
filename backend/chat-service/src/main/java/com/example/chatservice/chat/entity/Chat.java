package com.example.chatservice.chat.entity;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.common.domain.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
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
        setMessage(chatDto.getMessage());
        setRoomId(chatDto.getRoomId());
        setSender(chatDto.getSender());
        setChatTime(chatDto.getChatTime());
        setChatType(chatDto.getChatType());
    }
}
