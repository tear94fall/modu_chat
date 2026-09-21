package com.example.chatservice.chat.entity;

import com.example.chatservice.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 하나에 한 사람이 남긴 반응. 한 사람은 한 메시지에 하나만 남길 수 있다(유니크).
 * userId 는 Chat.sender 와 같은 값(구글 sub)이라 "내 메시지인지" 를 문자열 비교로 안다.
 */
@Getter
@Entity
@Table(name = "chat_reaction",
        indexes = @Index(name = "idx_chat_reaction_chat_id", columnList = "chat_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_reaction_chat_user", columnNames = {"chat_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReaction extends BaseTimeEntity {

    @Id
    @Column(name = "chat_reaction_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReactionEmoji emoji;

    public ChatReaction(Long chatId, String roomId, String userId, ReactionEmoji emoji) {
        this.chatId = chatId;
        this.roomId = roomId;
        this.userId = userId;
        this.emoji = emoji;
    }

    public void change(ReactionEmoji emoji) {
        this.emoji = emoji;
    }
}
