package com.example.memberservice.notice.dto

/** push-service 의 RequestPushMessage 와 같은 모양. 전체 발송에 쓴다. */
class PushMessageDto(
    val title: String?,
    val body: String?,
    val data: Map<String, String>?,
    val image: String?,
)
