package com.example.chatservice.chat.entity

import com.example.chatservice.chat.dto.ChatDto
import com.example.chatservice.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Chat protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "chat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var chatType: Int = 0
        protected set

    var roomId: String? = null
        protected set

    var sender: String? = null
        protected set

    var message: String? = null
        protected set

    var chatTime: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    var chatRoom: ChatRoom? = null
        protected set

    fun addChatRoom(chatRoom: ChatRoom) {
        this.chatRoom = chatRoom
    }

    constructor(msg: String?) : this() {
        this.message = msg
    }

    constructor(msg: String?, chatRoom: ChatRoom?) : this() {
        this.message = msg
        this.chatRoom = chatRoom
    }

    constructor(msg: String?, roomId: String?, chatRoom: ChatRoom?, sender: String?, chatTime: String?, type: Int) : this() {
        this.message = msg
        this.roomId = roomId
        this.chatRoom = chatRoom
        this.sender = sender
        this.chatTime = chatTime
        this.chatType = type
    }

    constructor(chatDto: ChatDto) : this() {
        this.message = chatDto.message
        this.roomId = chatDto.roomId
        this.sender = chatDto.sender
        this.chatTime = chatDto.chatTime
        this.chatType = chatDto.chatType
    }
}
