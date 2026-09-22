package com.example.chatstoreservice.chat.dto

import java.io.Serializable

data class ChatRoomDto(
    var roomId: String? = null,
    var roomName: String? = null,
    var roomImage: String? = null,
    var lastChatMsg: String? = null,
    var lastChatId: String? = null,
    var lastChatTime: String? = null,
    var members: MutableList<MemberDto> = mutableListOf(),
) : Serializable {

    fun updateLastChat(chatId: String, chatDto: ChatDto) {
        lastChatId = chatId
        lastChatMsg = if (chatDto.chatType == ChatType.TEXT.chatType) chatDto.message else ChatType.fromChatType(chatDto.chatType)?.chatTypeStr
        lastChatTime = chatDto.chatTime
    }

    /** true 면 방 멤버가 아니다(옛 자바 이름 그대로). */
    fun checkChatRoomMember(userId: String): Boolean = members.none { it.userId == userId }
}
