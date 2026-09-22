package com.example.chatservice.chat.entity

import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.common.domain.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class ChatRoom protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "chat_room_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var roomId: String? = null
        protected set

    @Column(nullable = false)
    var roomName: String? = null
        protected set

    @Column(nullable = false)
    var roomImage: String? = null
        protected set

    @Column(nullable = false)
    var lastChatMsg: String? = null
        protected set

    @Column(nullable = false)
    var lastChatId: String? = null
        protected set

    @Column(nullable = false)
    var lastChatTime: String? = null
        protected set

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL])
    var chatRoomMemberList: MutableList<ChatRoomMember> = ArrayList()
        protected set

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL])
    var chat: MutableList<Chat> = ArrayList()
        protected set

    fun addChatting(chat: Chat) {
        this.chat.add(chat)
        this.lastChatId = chat.id.toString()
        this.lastChatMsg = chat.message
    }

    fun removeChat(chat: Chat) {
        this.chat.remove(chat)
        this.lastChatId = if (this.chat.isNotEmpty()) this.chat[this.chat.size - 1].toString() else ""
        this.lastChatMsg = "삭제된 메시지 입니다."
    }

    fun updateChatRoom(chatRoomDto: ChatRoomDto) {
        this.roomName = chatRoomDto.roomName
        this.roomImage = chatRoomDto.roomImage
        this.lastChatMsg = chatRoomDto.lastChatMsg
        this.lastChatId = chatRoomDto.lastChatId
        this.lastChatTime = chatRoomDto.lastChatTime
    }

    constructor(roomName: String?) : this() {
        this.roomName = roomName
    }

    constructor(roomId: String?, roomName: String?, roomImage: String?, lastChatMsg: String?, lastChatId: String?, lastChatTime: String?) : this() {
        this.roomId = roomId
        this.roomName = roomName
        this.roomImage = roomImage
        this.lastChatMsg = lastChatMsg
        this.lastChatId = lastChatId
        this.lastChatTime = lastChatTime
    }

    constructor(chatRoomDto: ChatRoomDto) : this() {
        this.roomId = chatRoomDto.roomId
        this.roomName = chatRoomDto.roomName
        this.roomImage = chatRoomDto.roomImage
        this.lastChatMsg = chatRoomDto.lastChatMsg
        this.lastChatId = chatRoomDto.lastChatId
        this.lastChatTime = chatRoomDto.lastChatTime
    }
}
