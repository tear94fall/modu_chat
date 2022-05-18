package com.example.modumessenger.chat.entity;

import com.example.modumessenger.common.domain.BaseTimeEntity;
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

    public enum ChatType {
        ENTER, COMM
    }

    private ChatType chatType;
    private String roomId;
    private String sender;
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    @ToString.Exclude
    private ChatRoom chatRoom;

    public Chat(String msg) { this.message = msg; }

    public Chat(String msg, ChatRoom chatRoom) {
        this.message = msg;
        this.chatRoom = chatRoom;
    }

    public Chat(String msg, String roomId, ChatRoom chatRoom, String sender) {
        this.message = msg;
        this.roomId = roomId;
        this.chatRoom = chatRoom;
        this.sender = sender;
    }
}
