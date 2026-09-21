package com.example.chatstoreservice.chat.dto

import java.io.Serializable

/** chat-service 내부 API 응답. 모든 필드에 기본값이 있어 Jackson 이 기본 생성자로 만든다. */
data class ChatDto(
    var id: Long? = null,
    var chatType: Int = 0,
    var roomId: String? = null,
    var sender: String? = null,
    var message: String? = null,
    var chatTime: String? = null,
    var chatRoomDto: ChatRoomDto? = null,
) : Serializable
