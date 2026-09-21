package com.example.pushservice.api.debug

import com.example.pushservice.fcm.dto.RequestChatDto
import com.example.pushservice.fcm.dto.RequestPushMessage
import com.example.pushservice.fcm.service.FcmService
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 운영자용 브로드캐스트/테스트 발송. 게이트웨이 라우트가 없고 prod 에서는 빈이 생성되지 않는다.
 * 로컬/dev 에서 서비스 포트로 직접 호출한다.
 */
@Profile("!prod")
@RestController
@RequestMapping("/api-debug/push")
class PushDebugController(
    private val fcmService: FcmService,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    @PostMapping("/topics/{topic}")
    fun notificationTopics(@PathVariable("topic") topic: String, @RequestBody data: RequestPushMessage) {
        fcmService.sendTopicMessage(topic, data.title, data.body, data.image)
    }

    @PostMapping("/users")
    fun notificationUsers(@RequestBody data: RequestPushMessage) {
        fcmService.broadcast(data, multicastMessageSize)
    }

    @PostMapping("/user/{userId}")
    fun notificationUser(@PathVariable("userId") userId: String, @RequestBody data: RequestPushMessage) {
        val fcmToken = fcmService.searchFcmToken(userId) ?: throw NoSuchElementException("no fcm token for $userId")
        fcmService.sendTargetMessage(fcmToken.fcmToken, data.title, data.body, data.image)
    }

    @PostMapping("/users/{token}")
    fun notificationToken(@PathVariable("token") token: String, @RequestBody chatDto: RequestChatDto) {
        val notification = Notification.builder().setTitle(chatDto.chatRoomName).setBody(chatDto.message).setImage(chatDto.image).build()
        val msg = Message.builder().setToken(token).setNotification(notification).build()
        fcmService.sendMessage(msg)
    }
}
