package com.example.modumessenger.chat.entity;

import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.common.domain.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
        setRoomId(chatRoomDto.getRoomId());
        setRoomName(chatRoomDto.getRoomName());
        setRoomImage(chatRoomDto.getRoomImage());
        setLastChatMsg(chatRoomDto.getLastChatMsg());
        setLastChatId(chatRoomDto.getLastChatId());
        setLastChatTime(chatRoomDto.getLastChatTime());
    }
}
