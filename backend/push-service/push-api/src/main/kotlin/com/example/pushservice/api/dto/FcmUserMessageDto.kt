package com.example.pushservice.api.dto

import com.example.pushservice.application.usecase.command.UserDataPushCommand
import io.swagger.v3.oas.annotations.media.Schema

/** 한 사람(userId)에게만 보내는 푸시. 등록된 토큰이 없으면 조용히 지나간다. */
@Schema(description = "한 회원에게 보내는 데이터 푸시(반응 알림 등). 알림 문구는 앱이 만든다")
data class FcmUserMessageDto(
    @field:Schema(description = "받는 회원 userId(모두 계정 subject). 필수", example = "108234567890123456789")
    var userId: String? = null,
    @field:Schema(description = "제목. data 의 title 로 실린다(없으면 빈 문자열)", example = "주말 등산 모임")
    var title: String? = null,
    @field:Schema(description = "본문. data 의 message 로 실린다(없으면 빈 문자열)", example = "소율님이 ❤️ 반응을 남겼습니다")
    var body: String? = null,
    @field:Schema(description = "앱에 함께 보낼 키-값(roomId, kind 등). title/message 키를 덮어쓸 수 있다", example = "{\"kind\":\"REACTION\"}")
    var data: Map<String, String>? = null,
) {
    fun toCommand(): UserDataPushCommand = UserDataPushCommand(userId, title, body, data)
}
