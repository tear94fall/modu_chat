package com.example.chatservice.api.dto

import com.example.chatservice.application.usecase.command.UpdateChatRoomCommand
import com.example.chatservice.application.usecase.result.ChatRoomView
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

    fun toCommand() = UpdateChatRoomCommand(
        roomName = roomName,
        roomImage = roomImage,
        lastChatMsg = lastChatMsg,
        lastChatId = lastChatId,
        lastChatTime = lastChatTime,
    )

    companion object {
        fun of(view: ChatRoomView) = ChatRoomDto(
            id = view.room.id,
            roomId = view.room.roomId,
            roomName = view.room.roomName,
            roomImage = view.room.roomImage,
            lastChatMsg = view.room.lastChatMsg,
            lastChatId = view.room.lastChatId,
            lastChatTime = view.room.lastChatTime,
            members = view.members.map { MemberDto.of(it) }.toMutableList(),
        )
    }
}
