package com.example.wsservice.chat.dto

import java.io.Serializable

/** 앱과 소켓으로 주고받는 채팅. Kotlin 모듈 없는 ObjectMapper 도 읽도록 기본값 + var 다. */
data class ChatDto(
    var id: Long? = null,
    var chatType: Int = 0,
    var roomId: String? = null,
    var sender: String? = null,
    var message: String? = null,
    var chatTime: String? = null,
    var chatRoomDto: ChatRoomDto? = null,
    /** 이모지별 반응 집계(chat-service 가 채운다). 새 메시지 브로드캐스트에는 비어 있다. */
    var reactions: List<ReactionSummaryDto>? = null,
) : Serializable
