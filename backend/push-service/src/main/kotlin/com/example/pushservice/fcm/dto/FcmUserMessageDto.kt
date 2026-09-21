package com.example.pushservice.fcm.dto

/** 한 사람(userId)에게만 보내는 푸시. 등록된 토큰이 없으면 조용히 지나간다. */
data class FcmUserMessageDto(
    var userId: String? = null,
    var title: String? = null,
    var body: String? = null,
    var data: Map<String, String>? = null,
)
