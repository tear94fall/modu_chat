package com.example.wsservice.fcm.service

import com.example.wsservice.fcm.client.FcmFeignClient
import com.example.wsservice.fcm.dto.FcmMessageDto
import com.example.wsservice.fcm.dto.FcmUserMessageDto
import org.springframework.stereotype.Service

@Service
class FcmService(private val fcmFeignClient: FcmFeignClient) {

    fun sendFcmMessage(fcmMessageDto: FcmMessageDto) {
        fcmFeignClient.sendMessage(fcmMessageDto)
    }

    fun sendUserMessage(fcmUserMessageDto: FcmUserMessageDto) {
        fcmFeignClient.sendUserMessage(fcmUserMessageDto)
    }
}
