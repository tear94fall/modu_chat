package com.example.wsservice.chat.service

import com.example.wsservice.chat.client.ChatFeignClient
import com.example.wsservice.chat.dto.ChatRoomDto
import org.springframework.stereotype.Service

@Service
class ChatRoomService(private val chatFeignClient: ChatFeignClient) {

    fun getChatRoom(roomId: String): ChatRoomDto = chatFeignClient.getChatRoom(roomId)

    fun updateChatRoom(roomId: String, chatRoomDto: ChatRoomDto): ChatRoomDto = chatFeignClient.updateChatRoom(roomId, chatRoomDto)

    fun updateLastReadChat(roomId: String, userId: String) = chatFeignClient.updateLastReadChat(roomId, userId)
}
