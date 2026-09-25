package com.example.chatservice.api.admin.dto

import com.example.chatservice.chat.entity.ChatRoom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdminChatRoomSummaryDto(room: ChatRoom) {
    val id: Long? = room.id
    val roomId: String? = room.roomId
    val roomName: String? = room.roomName
    val roomImage: String? = room.roomImage
    val memberCount: Int = room.chatRoomMemberList.size
    val lastChatMsg: String? = room.lastChatMsg
    val lastChatTime: String? = room.lastChatTime

    /** 방을 만든 시각. 채팅 시각과 같은 UTC `yyyy-MM-dd HH:mm:ss` 다(서버 시계가 UTC). 백오피스가 보는 사람의 시간대로 바꾼다. */
    val createdDate: String? = AdminTime.format(room.createdDate)
}

/** 백오피스 응답의 시각 형식. 앱·소켓이 주고받는 채팅 시각과 같은 모양의 UTC 문자열로 맞춘다. */
object AdminTime {
    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun format(time: LocalDateTime?): String? = time?.format(FORMATTER)
}
