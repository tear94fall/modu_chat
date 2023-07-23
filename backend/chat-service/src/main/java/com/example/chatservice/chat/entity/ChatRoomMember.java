package com.example.chatservice.chat.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomMember {

    @Id
    @Column(name = "chat_room_member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    public ChatRoomMember(Long memberId, ChatRoom chatRoom) {
        setMemberId(memberId);
        setChatRoom(chatRoom);
    }
}
