package com.example.pushservice.application.push

/** 알림 영역(제목·본문·이미지). 데이터 전용 메시지에는 붙이지 않는다. */
data class PushNotification(
    val title: String? = null,
    val body: String? = null,
    val image: String? = null,
)

/**
 * 푸시를 실제로 내보내는 바깥(FCM) 포트. 구현은 push-api 의 FirebasePushSender.
 * 발송이 거절되면 구현이 던지는 예외가 그대로 올라간다(응답 500).
 */
interface PushSender {

    /** 기기 토큰 하나에 보낸다. */
    fun sendToToken(token: String?, notification: PushNotification?, data: Map<String, String> = emptyMap())

    /** topic 을 구독한 기기에 보낸다. */
    fun sendToTopic(topic: String?, notification: PushNotification?, data: Map<String, String> = emptyMap())

    /** 여러 토큰에 한 번에 보낸다(멀티캐스트 한 묶음). 개별 토큰 실패는 무시하고 로그만 남긴다. */
    fun sendToTokens(tokens: List<String>, notification: PushNotification, data: Map<String, String> = emptyMap())
}
