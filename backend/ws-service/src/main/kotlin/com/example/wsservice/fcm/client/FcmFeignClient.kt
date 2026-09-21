package com.example.wsservice.fcm.client

import com.example.wsservice.fcm.dto.FcmMessageDto
import com.example.wsservice.fcm.dto.FcmUserMessageDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("push-service")
interface FcmFeignClient {

    @PostMapping("/api-internal/push/chat")
    fun sendMessage(@RequestBody fcmMessageDto: FcmMessageDto): Void?

    @PostMapping("/api-internal/push/user")
    fun sendUserMessage(@RequestBody fcmUserMessageDto: FcmUserMessageDto): Void?
}
