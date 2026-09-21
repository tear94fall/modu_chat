package com.example.wsservice.fcm.dto

import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatRoomDto

/** push-service 에 보내는 방(topic) 푸시. */
class FcmMessageDto() {
    var topic: String? = null
    var type: Int = 0
    var title: String? = null
    var body: String? = null
    var image: String? = null
    var data: Map<String, String>? = null

    constructor(chatRoomDto: ChatRoomDto, chatDto: ChatDto) : this() {
        topic = chatRoomDto.roomId
        type = chatDto.chatType
        title = chatRoomDto.roomName
        body = chatDto.message
        image = null
        val members = chatRoomDto.members.orEmpty()
        val senderName = members.firstOrNull { chatDto.sender != null && chatDto.sender == it.userId }?.username ?: ""
        val map = HashMap<String, String?>()
        map["roomId"] = chatRoomDto.roomId
        map["sender"] = chatDto.sender
        // 앱이 알림 제목/본문을 만들 때 쓴다. 수신자가 발신자에게 별칭을 붙여 두었으면 앱이 senderName 대신 별칭을 쓴다.
        map["senderName"] = senderName
        map["memberCount"] = members.size.toString()
        @Suppress("UNCHECKED_CAST")
        data = map as Map<String, String>
    }

    constructor(topic: String?, type: Int, title: String?, body: String?, image: String?, sender: String?) : this() {
        this.topic = topic
        this.type = type
        this.title = title
        this.body = body
        this.image = image
        val map = HashMap<String, String?>()
        map["roomId"] = topic
        map["sender"] = sender
        @Suppress("UNCHECKED_CAST")
        data = map as Map<String, String>
    }
}
