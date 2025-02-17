package com.example.chatservice.chat.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @Column(name = "chat_room_member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private String lastReadChatId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    public void updateLastReadChatId(String lastReadChatId) {
        this.lastReadChatId = lastReadChatId;
    }

    public ChatRoomMember(Long memberId, String lastReadChatId, ChatRoom chatRoom) {
        this.memberId = memberId;
        this.lastReadChatId = lastReadChatId;
        this.chatRoom = chatRoom;
    }
}
