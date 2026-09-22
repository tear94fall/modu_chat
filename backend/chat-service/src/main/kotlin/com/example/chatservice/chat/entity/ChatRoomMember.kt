package com.example.chatservice.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    indexes = [Index(name = "idx_chat_room_member_member_id", columnList = "member_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_room_member", columnNames = ["chat_room_id", "member_id"])],
)
class ChatRoomMember protected constructor() {

    @Id
    @Column(name = "chat_room_member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var memberId: Long? = null
        protected set

    var lastReadChatId: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    var chatRoom: ChatRoom? = null
        protected set

    fun updateLastReadChatId(lastReadChatId: String?) {
        this.lastReadChatId = lastReadChatId
    }

    constructor(memberId: Long?, lastReadChatId: String?, chatRoom: ChatRoom?) : this() {
        this.memberId = memberId
        this.lastReadChatId = lastReadChatId
        this.chatRoom = chatRoom
    }
}
