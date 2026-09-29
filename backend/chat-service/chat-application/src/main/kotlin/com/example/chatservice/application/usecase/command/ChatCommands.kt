package com.example.chatservice.application.usecase.command

/** 메시지 저장(ws-service · chat-store-service). chatTime 은 보낸 쪽이 찍은 UTC 문자열을 그대로 둔다. */
data class SaveChatCommand(
    val chatType: Int,
    val roomId: String?,
    val sender: String?,
    val message: String?,
    val chatTime: String?,
)

/** 방 정보 수정. 다섯 값을 통째로 덮어쓴다 — 컬럼이 모두 NOT NULL 이라 빠진 값이 있으면 400 이다. */
data class UpdateChatRoomCommand(
    val roomName: String?,
    val roomImage: String?,
    val lastChatMsg: String?,
    val lastChatId: String?,
    val lastChatTime: String?,
) {
    init {
        require(roomName != null && roomImage != null && lastChatMsg != null && lastChatId != null && lastChatTime != null) {
            "roomName, roomImage, lastChatMsg, lastChatId, lastChatTime 을 모두 보내야 한다"
        }
    }
}
