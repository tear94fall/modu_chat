package com.example.chatservice.chat.dto

import java.io.Serializable

data class ChatDto(
    var id: Long? = null,
    var chatType: Int = 0,
    var roomId: String? = null,
    var sender: String? = null,
    var message: String? = null,
    var chatTime: String? = null,
    var chatRoomDto: ChatRoomDto? = null,
    /** 이모지별 반응 집계. 이력 조회에서만 채워진다(소켓 프레임의 채팅에는 없음 — 새 메시지는 반응이 없다). */
    var reactions: List<ReactionSummaryDto>? = null,
) : Serializable {

    constructor(msg: String?, roomId: String?, sender: String?, chatTime: String?, type: Int, chatRoomDto: ChatRoomDto?) : this(
        id = null,
        chatType = type,
        roomId = roomId,
        sender = sender,
        message = msg,
        chatTime = chatTime,
        chatRoomDto = chatRoomDto,
    )
}
