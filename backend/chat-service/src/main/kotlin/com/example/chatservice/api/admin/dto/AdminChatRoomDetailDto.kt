package com.example.chatservice.api.admin.dto

import com.example.chatservice.chat.dto.ChatRoomDto
import com.example.chatservice.member.dto.MemberDto
import com.example.chatservice.chat.entity.ChatRoom

/** 백오피스 방 상세. 앱이 쓰는 [ChatRoomDto] 는 그대로 두고 생성 시각만 더한다. */
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
        fun of(room: ChatRoom, members: List<MemberDto>): AdminChatRoomDetailDto {
            val dto = ChatRoomDto(room, members)
            return AdminChatRoomDetailDto(
                id = dto.id,
                roomId = dto.roomId,
                roomName = dto.roomName,
                roomImage = dto.roomImage,
                lastChatMsg = dto.lastChatMsg,
                lastChatId = dto.lastChatId,
                lastChatTime = dto.lastChatTime,
                members = dto.members,
                createdDate = AdminTime.format(room.createdDate),
            )
        }
    }
}
