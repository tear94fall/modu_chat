package com.example.pushservice.fcm.dto

/** ws-service 가 방(topic) 전체에 보내는 채팅 푸시. */
data class FcmMessageDto(
    var topic: String? = null,
    var type: Int = 0,
    var title: String? = null,
    var body: String? = null,
    var image: String? = null,
    var data: Map<String, String>? = null,
)
