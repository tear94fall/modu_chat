package com.example.chatservice.chat.entity

import com.example.chatservice.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 메시지 하나에 한 사람이 남긴 반응. 한 사람은 한 메시지에 하나만 남길 수 있다(유니크).
 * userId 는 Chat.sender 와 같은 값(구글 sub)이라 "내 메시지인지" 를 문자열 비교로 안다.
 */
@Entity
@Table(
    name = "chat_reaction",
    indexes = [Index(name = "idx_chat_reaction_chat_id", columnList = "chat_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_reaction_chat_user", columnNames = ["chat_id", "user_id"])],
)
class ChatReaction protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "chat_reaction_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "chat_id", nullable = false)
    var chatId: Long? = null
        protected set

    @Column(nullable = false)
    var roomId: String? = null
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var emoji: ReactionEmoji? = null
        protected set

    constructor(chatId: Long?, roomId: String?, userId: String?, emoji: ReactionEmoji?) : this() {
        this.chatId = chatId
        this.roomId = roomId
        this.userId = userId
        this.emoji = emoji
    }

    fun change(emoji: ReactionEmoji) {
        this.emoji = emoji
    }
}
