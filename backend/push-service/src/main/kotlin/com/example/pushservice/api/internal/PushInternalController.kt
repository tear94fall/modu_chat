package com.example.pushservice.api.internal

import com.example.pushservice.fcm.dto.FcmMessageDto
import com.example.pushservice.fcm.dto.FcmUserMessageDto
import com.example.pushservice.fcm.dto.RequestPushMessage
import com.example.pushservice.fcm.service.FcmService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** ws-service, member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal/push")
class PushInternalController(
    private val fcmService: FcmService,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    /** 회원 탈퇴(member-service). 그 회원의 FCM 토큰을 지운다. */
    @DeleteMapping("/token/{userId}")
    fun deleteToken(@PathVariable("userId") userId: String): ResponseEntity<Void> {
        fcmService.deleteFcmToken(userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/chat")
    fun pushMessage(@RequestBody fcmMessageDto: FcmMessageDto): ResponseEntity<Void> {
        fcmService.sendTopicMessageWithData(fcmMessageDto)
        return ResponseEntity.ok().build()
    }

    /** ws-service 가 반응 알림처럼 한 사람에게만 보낼 때. 토큰이 없으면 204. */
    @PostMapping("/user")
    fun pushUser(@RequestBody dto: FcmUserMessageDto): ResponseEntity<Void> =
        if (fcmService.sendUserMessageWithData(dto)) ResponseEntity.ok().build() else ResponseEntity.noContent().build()

    /** 공지를 올린 서비스가 전체 발송을 맡길 때 쓴다. 백오피스의 /api-admin/push/broadcast 와 같은 동작이다. */
    @PostMapping("/broadcast")
    fun broadcast(@RequestBody data: RequestPushMessage): ResponseEntity<Map<String, Int>> {
        val groups = fcmService.broadcast(data, multicastMessageSize)
        return ResponseEntity.ok(mapOf("groups" to groups))
    }
}
