package com.example.wsservice.chat.service

import com.example.wsservice.chat.client.ChatFeignClient
import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ReactionRequestDto
import com.example.wsservice.chat.dto.ReactionResultDto
import org.springframework.stereotype.Service

@Service
class ChatService(private val chatFeignClient: ChatFeignClient) {

    fun saveChat(chatDto: ChatDto): Long = chatFeignClient.saveChat(chatDto)

    fun getChat(chatId: String): ChatDto = chatFeignClient.getChat(chatId)

    /** 반응 토글. chat-service 가 거부하면(내 메시지·모르는 이모지) FeignException 이 난다. */
    fun react(roomId: String, chatId: String, userId: String, emoji: String): ReactionResultDto =
        chatFeignClient.react(roomId, chatId, userId, ReactionRequestDto(emoji))
}
