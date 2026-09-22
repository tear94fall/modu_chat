package com.example.chatstoreservice.chat.client

import com.example.chatstoreservice.chat.dto.ChatDto
import com.example.chatstoreservice.chat.dto.ChatRoomDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient("chat-service")
interface ChatFeignClient {

    @GetMapping("/api-internal/chat/{chatId}")
    fun getChat(@PathVariable("chatId") chatId: String): ChatDto

    @GetMapping("/api-internal/chat/{roomId}/room")
    fun getChatRoom(@PathVariable("roomId") roomId: String): ChatRoomDto
}
