package com.example.pushservice.fcm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RequestChatDto(
    var chatRoomName: String? = null,
    var message: String? = null,
    /** 옛 자바 필드 이름이 대문자 `Image` 였다. JSON 키를 그대로 유지한다. */
    @field:JsonProperty("Image") var image: String? = null,
)
