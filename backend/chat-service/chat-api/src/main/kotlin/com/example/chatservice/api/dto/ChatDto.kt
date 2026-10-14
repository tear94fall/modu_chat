package com.example.chatservice.api.dto

import com.example.chatservice.application.usecase.command.SaveChatCommand
import com.example.chatservice.application.usecase.result.ChatResult
import java.io.Serializable

data class ChatDto(
    var id: Long? = null,
    var chatType: Int = 0,
    var roomId: String? = null,
    var sender: String? = null,
    var message: String? = null,
    var chatTime: String? = null,
    /** 예전 응답 모양을 지키려고 남겨 둔 자리. 서버는 채우지 않는다(항상 null). */
    var chatRoomDto: ChatRoomDto? = null,
    /** 이모지별 반응 집계. 이력 조회에서만 채워진다(소켓 프레임의 채팅에는 없음 — 새 메시지는 반응이 없다). */
    var reactions: List<ReactionSummaryDto>? = null,
) : Serializable {

    fun toCommand() = SaveChatCommand(chatType = chatType, roomId = roomId, sender = sender, message = message, chatTime = chatTime)

    companion object {
        fun of(chat: ChatResult) = ChatDto(
            id = chat.id,
            chatType = chat.chatType,
            roomId = chat.roomId,
            sender = chat.sender,
            message = chat.message,
            chatTime = chat.chatTime,
            reactions = chat.reactions?.map { ReactionSummaryDto.of(it) },
        )
    }
}
