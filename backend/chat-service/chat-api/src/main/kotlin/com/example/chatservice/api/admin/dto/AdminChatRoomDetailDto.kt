package com.example.chatservice.api.admin.dto

import com.example.chatservice.api.dto.MemberDto
import com.example.chatservice.application.usecase.result.ChatRoomView

/** 백오피스 방 상세. 앱이 쓰는 ChatRoomDto 와 같은 값에 생성 시각만 더한다. */
data class AdminChatRoomDetailDto(
    val id: Long?,
    val roomId: String?,
    val roomName: String?,
    val roomImage: String?,
    val lastChatMsg: String?,
    val lastChatId: String?,
    val lastChatTime: String?,
    val members: List<MemberDto>,
    /** UTC `yyyy-MM-dd HH:mm:ss`. */
    val createdDate: String?,
) {
    companion object {
        fun of(view: ChatRoomView) = AdminChatRoomDetailDto(
            id = view.room.id,
            roomId = view.room.roomId,
            roomName = view.room.roomName,
            roomImage = view.room.roomImage,
            lastChatMsg = view.room.lastChatMsg,
            lastChatId = view.room.lastChatId,
            lastChatTime = view.room.lastChatTime,
            members = view.members.map { MemberDto.of(it) },
            createdDate = AdminTime.format(view.room.createdDate),
        )
    }
}
