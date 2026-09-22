package com.example.pushservice.api.admin

import com.example.pushservice.fcm.dto.RequestPushMessage
import com.example.pushservice.fcm.service.FcmService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 백오피스 푸시 발송. 게이트웨이(ROLE_ADMIN JWT) 경유, InternalApiFilter 가 토큰 검사. */
@RestController
@RequestMapping("/api-admin/push")
class PushAdminController(
    private val fcmService: FcmService,
    @Value("\${project.properties.firebase-multicast-message-size}") private val multicastMessageSize: Long,
) {

    @PostMapping("/broadcast")
    fun broadcast(@RequestBody data: RequestPushMessage): ResponseEntity<Map<String, Int>> {
        val groups = fcmService.broadcast(data, multicastMessageSize)
        return ResponseEntity.ok(mapOf("groups" to groups))
    }

    @PostMapping("/users/{userId}")
    fun sendToUser(@PathVariable("userId") userId: String, @RequestBody data: RequestPushMessage): ResponseEntity<Void> {
        val token = fcmService.searchFcmToken(userId) ?: return ResponseEntity.notFound().build()
        fcmService.sendTargetMessage(token.fcmToken, data.title, data.body, data.image)
        return ResponseEntity.ok().build()
    }
}
