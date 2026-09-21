package com.example.wsservice.chat.client

import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatRoomDto
import com.example.wsservice.chat.dto.ReactionRequestDto
import com.example.wsservice.chat.dto.ReactionResultDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("chat-service")
interface ChatFeignClient {

    @PostMapping("/api-internal/chat")
    fun saveChat(@RequestBody chatDto: ChatDto): Long

    @GetMapping("/api-internal/chat/{chatId}")
    fun getChat(@PathVariable("chatId") chatId: String): ChatDto

    @GetMapping("/api-internal/chat/{roomId}/room")
    fun getChatRoom(@PathVariable("roomId") roomId: String): ChatRoomDto

    @PostMapping("/api-internal/chat/{roomId}/room")
    fun updateChatRoom(@PathVariable("roomId") roomId: String, @RequestBody chatRoomDto: ChatRoomDto): ChatRoomDto

    @PostMapping("/api-internal/chat/reaction/{roomId}/{chatId}/{userId}")
    fun react(
        @PathVariable("roomId") roomId: String,
        @PathVariable("chatId") chatId: String,
        @PathVariable("userId") userId: String,
        @RequestBody request: ReactionRequestDto,
    ): ReactionResultDto

    @PostMapping("/api-internal/chat/read/{roomId}/{userId}")
    fun updateLastReadChat(@PathVariable("roomId") roomId: String, @PathVariable("userId") userId: String)
}
