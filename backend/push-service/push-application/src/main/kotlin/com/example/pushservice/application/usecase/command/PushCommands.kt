package com.example.pushservice.application.usecase.command

/** 알림(제목·본문·이미지) 발송 내용. data 는 전체 발송에서만 쓰인다. */
data class NotificationCommand(
    val title: String? = null,
    val body: String? = null,
    val image: String? = null,
    val data: Map<String, String>? = null,
)

/** 채팅방(topic) 데이터 푸시. */
data class TopicDataPushCommand(
    val topic: String?,
    val type: Int,
    val title: String?,
    val body: String?,
    val data: Map<String, String>? = null,
)

/** 한 사람(userId) 데이터 푸시. */
data class UserDataPushCommand(
    val userId: String?,
    val title: String?,
    val body: String?,
    val data: Map<String, String>? = null,
)
