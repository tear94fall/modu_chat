package com.example.wsservice.fcm.dto

/** 방(topic)이 아니라 한 사람에게만 보내는 푸시. 반응 알림은 메시지 작성자 한 명에게만 간다. */
data class FcmUserMessageDto(
    var userId: String? = null,
    var title: String? = null,
    var body: String? = null,
    var data: Map<String, String>? = null,
)
