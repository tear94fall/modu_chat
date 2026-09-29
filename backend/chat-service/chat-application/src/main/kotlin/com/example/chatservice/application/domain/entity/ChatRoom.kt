package com.example.chatservice.application.domain.entity

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

    /**
     * 메시지가 지워졌다. 마지막 메시지 id 는 남은 것 중 가장 최근 것으로, 미리보기는 삭제 안내로 바꾼다.
     * [lastRemainingChatId] 는 남은 메시지가 없으면 null 이다(그때 id 는 빈 문자열).
     */
    fun chatDeleted(lastRemainingChatId: Long?) {
        this.lastChatId = lastRemainingChatId?.toString() ?: ""
        this.lastChatMsg = DELETED_CHAT_MESSAGE
    }

    fun updateChatRoom(roomName: String?, roomImage: String?, lastChatMsg: String?, lastChatId: String?, lastChatTime: String?) {
        this.roomName = roomName
        this.roomImage = roomImage
        this.lastChatMsg = lastChatMsg
        this.lastChatId = lastChatId
        this.lastChatTime = lastChatTime
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

    companion object {
        const val DELETED_CHAT_MESSAGE = "삭제된 메시지 입니다."
    }
}
