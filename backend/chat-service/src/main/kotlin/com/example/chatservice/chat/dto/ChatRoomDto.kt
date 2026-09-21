package com.example.chatservice.chat.dto

import com.example.chatservice.chat.entity.ChatRoom
import com.example.chatservice.member.dto.MemberDto
import java.io.Serializable

data class ChatRoomDto(
    var id: Long? = null,
    var roomId: String? = null,
    var roomName: String? = null,
    var roomImage: String? = null,
    var lastChatMsg: String? = null,
    var lastChatId: String? = null,
    var lastChatTime: String? = null,
    var members: MutableList<MemberDto> = ArrayList(),
) : Serializable {

    constructor(chatRoom: ChatRoom, members: List<MemberDto>) : this(
        id = chatRoom.id,
        roomId = chatRoom.roomId,
        roomName = chatRoom.roomName,
        roomImage = chatRoom.roomImage,
        lastChatMsg = chatRoom.lastChatMsg,
        lastChatId = chatRoom.lastChatId,
        lastChatTime = chatRoom.lastChatTime,
        members = members.toMutableList(),
    )

    fun updateLastChat(chatId: String?, chatDto: ChatDto) {
        lastChatId = chatId
        lastChatMsg = if (chatDto.chatType == ChatType.TEXT.chatType) {
            chatDto.message
        } else {
            ChatType.fromChatType(chatDto.chatType)?.chatTypeStr
        }
        lastChatTime = chatDto.chatTime
    }

    fun checkChatRoomMember(userId: String): Boolean = members.none { it.userId == userId }
}
