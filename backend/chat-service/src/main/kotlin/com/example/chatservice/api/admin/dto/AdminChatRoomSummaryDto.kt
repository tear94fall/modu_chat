package com.example.chatservice.api.admin.dto

import com.example.chatservice.chat.entity.ChatRoom

class AdminChatRoomSummaryDto(room: ChatRoom) {
    val id: Long? = room.id
    val roomId: String? = room.roomId
    val roomName: String? = room.roomName
    val roomImage: String? = room.roomImage
    val memberCount: Int = room.chatRoomMemberList.size
    val lastChatMsg: String? = room.lastChatMsg
    val lastChatTime: String? = room.lastChatTime
}
