package com.example.chatservice.chat.dto

data class ChatRoomLastReadChatDto(
    var roomId: String? = null,
    var lastSendChatId: Long? = null,
    var lastReadChatId: Long? = null,
    var unreadChatCount: Long? = null,
) {

    fun updateUnreadChatCount(unreadChatCount: Long?) {
        this.unreadChatCount = unreadChatCount
    }

    companion object {
        @JvmStatic
        fun createChatRoomLastReadChatDto(roomId: String?, lastSendChatId: Long?, lastReadChatId: Long?, unreadChatCount: Long?) =
            ChatRoomLastReadChatDto(roomId, lastSendChatId, lastReadChatId, unreadChatCount)
    }
}
