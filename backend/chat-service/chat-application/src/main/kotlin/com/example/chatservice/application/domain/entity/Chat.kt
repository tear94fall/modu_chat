package com.example.chatservice.application.domain.entity

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

    /** 보낸 사람의 userId(모두 계정 subject). member id(숫자 PK)가 아니다. */
    var sender: String? = null
        protected set

    var message: String? = null
        protected set

    /** 보낸 쪽(앱·ws-service)이 찍은 UTC `yyyy-MM-dd HH:mm:ss`. 서버는 고치지 않고 그대로 저장한다. */
    var chatTime: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    var chatRoom: ChatRoom? = null
        protected set

    fun addChatRoom(chatRoom: ChatRoom) {
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
}
