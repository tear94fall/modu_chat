package com.example.pushservice.fcm.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "디버그용 채팅 알림 내용")
data class RequestChatDto(
    @field:Schema(description = "알림 제목으로 쓸 방 이름", example = "주말 등산 모임")
    var chatRoomName: String? = null,
    @field:Schema(description = "알림 본문", example = "테스트 메시지")
    var message: String? = null,
    /** 옛 자바 필드 이름이 대문자 `Image` 였다. JSON 키를 그대로 유지한다. */
    @field:Schema(description = "알림 이미지 URL(선택). JSON 키는 대문자 Image", example = "https://example.com/photo.png")
    @field:JsonProperty("Image") var image: String? = null,
)
