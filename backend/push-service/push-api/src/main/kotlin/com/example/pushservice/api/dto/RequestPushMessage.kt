package com.example.pushservice.api.dto

import com.example.pushservice.application.usecase.command.NotificationCommand
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 푸시 내용. 제목·본문이 알림으로 뜨고 data 는 앱에 그대로 전달된다")
data class RequestPushMessage(
    @field:Schema(description = "알림 제목", example = "모두의 채팅 점검 안내")
    var title: String? = null,
    @field:Schema(description = "알림 본문", example = "오늘 새벽 2시부터 30분간 점검합니다.")
    var body: String? = null,
    @field:Schema(description = "앱에 함께 보낼 키-값(선택). 전체 발송에서만 쓰이고 한 사람 발송·주제 발송에서는 무시된다", example = "{\"kind\":\"NOTICE\"}")
    var data: Map<String, String>? = null,
    @field:Schema(description = "알림에 붙일 이미지 URL(선택)", example = "https://example.com/banner.png")
    var image: String? = null,
) {
    fun toCommand(): NotificationCommand = NotificationCommand(title, body, image, data)
}
