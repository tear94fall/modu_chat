package com.example.pushservice.fcm.entity

/** FCM HTTP v1 요청 본문 모양(현재는 firebase-admin 을 써서 직접 쓰지 않는다). */
data class FcmMessage(
    @Suppress("PropertyName") val validate_only: Boolean = false,
    val message: Message? = null,
) {
    data class Message(val notification: Notification? = null, val token: String? = null)

    data class Notification(val title: String? = null, val body: String? = null, val image: String? = null)
}
