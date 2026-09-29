package com.example.chatservice.api.admin.dto

import com.example.chatservice.application.usecase.result.AdminChatRoomSummary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminChatRoomSummaryDto(
    val id: Long?,
    val roomId: String?,
    val roomName: String?,
    val roomImage: String?,
    val memberCount: Int,
    val lastChatMsg: String?,
    val lastChatTime: String?,
    /** 방을 만든 시각. 채팅 시각과 같은 UTC `yyyy-MM-dd HH:mm:ss` 다(서버 시계가 UTC). 백오피스가 보는 사람의 시간대로 바꾼다. */
    val createdDate: String?,
) {
    companion object {
        fun of(room: AdminChatRoomSummary) = AdminChatRoomSummaryDto(
            id = room.id,
            roomId = room.roomId,
            roomName = room.roomName,
            roomImage = room.roomImage,
            memberCount = room.memberCount,
            lastChatMsg = room.lastChatMsg,
            lastChatTime = room.lastChatTime,
            createdDate = AdminTime.format(room.createdDate),
        )
    }
}

/** 백오피스 응답의 시각 형식. 앱·소켓이 주고받는 채팅 시각과 같은 모양의 UTC 문자열로 맞춘다. */
object AdminTime {
    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun format(time: LocalDateTime?): String? = time?.format(FORMATTER)
}
