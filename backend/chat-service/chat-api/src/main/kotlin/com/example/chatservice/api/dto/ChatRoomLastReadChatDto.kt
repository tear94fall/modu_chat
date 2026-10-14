package com.example.chatservice.api.dto

import com.example.chatservice.application.usecase.result.UnreadCount

data class ChatRoomLastReadChatDto(
    var roomId: String? = null,
    var lastSendChatId: Long? = null,
    var lastReadChatId: Long? = null,
    var unreadChatCount: Long? = null,
) {
    companion object {
        fun of(unread: UnreadCount) =
            ChatRoomLastReadChatDto(unread.roomId, unread.lastSendChatId, unread.lastReadChatId, unread.unreadChatCount)
    }
}
