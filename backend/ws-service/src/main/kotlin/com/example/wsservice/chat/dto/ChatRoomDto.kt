package com.example.wsservice.chat.dto

import com.example.wsservice.member.dto.MemberDto
import java.io.Serializable

data class ChatRoomDto(
    var roomId: String? = null,
    var roomName: String? = null,
    var roomImage: String? = null,
    var lastChatMsg: String? = null,
    var lastChatId: String? = null,
    var lastChatTime: String? = null,
    var members: List<MemberDto>? = ArrayList(),
) : Serializable {

    fun updateLastChat(chatId: String, chatDto: ChatDto) {
        lastChatId = chatId
        lastChatMsg = if (chatDto.chatType == ChatType.TEXT.chatType) chatDto.message else ChatType.fromChatType(chatDto.chatType)?.chatTypeStr
        lastChatTime = chatDto.chatTime
    }

    /** true 면 방 멤버가 아니다(옛 자바 이름 그대로). */
    fun checkChatRoomMember(userId: String?): Boolean = members.orEmpty().none { it.userId == userId }
}
