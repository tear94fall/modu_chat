package com.example.memberservice.notice.client

import com.example.memberservice.notice.dto.PushMessageDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("push-service")
interface PushFeignClient {

    /** 회원 탈퇴: 그 회원의 FCM 토큰을 지운다. */
    @DeleteMapping("/api-internal/push/token/{userId}")
    fun deleteToken(@PathVariable("userId") userId: String): ResponseEntity<Void>

    @PostMapping("/api-internal/push/broadcast")
    fun broadcast(@RequestBody message: PushMessageDto): ResponseEntity<Map<String, Int>>
}
