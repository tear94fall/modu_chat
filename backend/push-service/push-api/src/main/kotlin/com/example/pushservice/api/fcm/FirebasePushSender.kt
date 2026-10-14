package com.example.pushservice.api.fcm

import com.example.pushservice.application.push.PushNotification
import com.example.pushservice.application.push.PushSender
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** [PushSender] 의 Firebase Admin SDK 구현. FirebaseApp 은 FirebaseConfig 가 기동 때 만든다. */
@Component
class FirebasePushSender : PushSender {

    private val log = LoggerFactory.getLogger(FirebasePushSender::class.java)

    override fun sendToToken(token: String?, notification: PushNotification?, data: Map<String, String>) {
        val builder = Message.builder().setToken(token).putAllData(data)
        notification?.let { builder.setNotification(it.toFirebase()) }
        send(builder.build())
    }

    override fun sendToTopic(topic: String?, notification: PushNotification?, data: Map<String, String>) {
        val builder = Message.builder().setTopic(topic).putAllData(data)
        notification?.let { builder.setNotification(it.toFirebase()) }
        send(builder.build())
    }

    override fun sendToTokens(tokens: List<String>, notification: PushNotification, data: Map<String, String>) {
        val message = MulticastMessage.builder()
            .putAllData(data)
            .setNotification(notification.toFirebase())
            .addAllTokens(tokens)
            .build()
        // sendMulticast()는 폐기된 FCM 배치 엔드포인트(/batch)를 호출해 404가 발생하므로
        // 메시지별로 개별 요청을 보내는 sendEachForMulticast()로 대체한다.
        val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
        log.info("sendEachForMulticast 완료: successCount={}, failureCount={}", response.successCount, response.failureCount)
    }

    private fun send(message: Message) {
        FirebaseMessaging.getInstance().send(message)
    }

    private fun PushNotification.toFirebase(): Notification =
        Notification.builder().setTitle(title).setBody(body).setImage(image).build()
}
