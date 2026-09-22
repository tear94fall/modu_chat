package com.example.chatservice.chat.dto

/** 방 멤버 한 명의 읽음 커서. 말풍선 옆 안 읽음 수 계산의 원본이다. */
data class ChatReadCursorDto(
    var userId: String? = null,
    var lastReadChatId: Long? = null,
)
