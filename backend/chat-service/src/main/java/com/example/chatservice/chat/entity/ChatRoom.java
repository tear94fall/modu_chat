package com.example.chatservice.chat.entity;

import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.common.domain.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @Column(name = "chat_room_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false)
    private String roomImage;

    @Column(nullable = false)
    private String lastChatMsg;

    @Column(nullable = false)
    private String lastChatId;

    @Column(nullable = false)
    private String lastChatTime;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ChatRoomMember> chatRoomMemberList = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Chat> chat = new ArrayList<>();

    public void addChatting(Chat chat) {
        this.chat.add(chat);
    }

    public void updateChatRoom(ChatRoomDto chatRoomDto) {
        this.roomName = chatRoomDto.getRoomName();
        this.roomImage = chatRoomDto.getRoomImage();
        this.lastChatMsg = chatRoomDto.getLastChatMsg();
        this.lastChatId = chatRoomDto.getLastChatId();
        this.lastChatTime = chatRoomDto.getLastChatTime();
    }

    public ChatRoom(String roomName) { this.roomName = roomName; }

    public ChatRoom(String roomId, String roomName, String roomImage, String lastChatMsg, String lastChatId, String lastChatTime) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomImage = roomImage;
        this.lastChatMsg = lastChatMsg;
        this.lastChatId = lastChatId;
        this.lastChatTime = lastChatTime;
    }

    public ChatRoom(ChatRoomDto chatRoomDto) {
        this.roomId = chatRoomDto.getRoomId();
        this.roomName = chatRoomDto.getRoomName();
        this.roomImage = chatRoomDto.getRoomImage();
        this.lastChatMsg = chatRoomDto.getLastChatMsg();
        this.lastChatId = chatRoomDto.getLastChatId();
        this.lastChatTime = chatRoomDto.getLastChatTime();
    }
}
