package com.example.pushservice.api.dto

import com.example.pushservice.application.usecase.command.TopicDataPushCommand
import io.swagger.v3.oas.annotations.media.Schema

/** ws-service 가 방(topic) 전체에 보내는 채팅 푸시. */
@Schema(description = "채팅방(topic) 전체에 보내는 채팅 푸시. 데이터 전용 메시지라 알림 문구는 앱이 만든다")
data class FcmMessageDto(
    @field:Schema(description = "FCM topic. 채팅방 id(UUID)를 쓴다", example = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b")
    var topic: String? = null,
    @field:Schema(description = "메시지 종류(채팅 chatType 값). data 의 type 으로 실린다", example = "0")
    var type: Int = 0,
    @field:Schema(description = "제목. data 의 title 로 실린다(보통 방 이름, 없으면 빈 문자열)", example = "주말 등산 모임")
    var title: String? = null,
    @field:Schema(description = "본문. data 의 message 로 실린다(없으면 빈 문자열)", example = "내일 몇 시에 볼까?")
    var body: String? = null,
    @field:Schema(description = "받기만 하고 쓰지 않는다")
    var image: String? = null,
    @field:Schema(
        description = "앱에 함께 보낼 키-값(roomId, sender, senderName, memberCount 등). type/title/message 키를 덮어쓸 수 있다",
        example = "{\"roomId\":\"3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b\",\"sender\":\"108234567890123456789\"}",
    )
    var data: Map<String, String>? = null,
) {
    fun toCommand(): TopicDataPushCommand = TopicDataPushCommand(topic, type, title, body, data)
}
