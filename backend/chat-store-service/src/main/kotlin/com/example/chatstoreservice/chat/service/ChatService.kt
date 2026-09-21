package com.example.chatstoreservice.chat.service

import com.example.chatstoreservice.chat.entity.Chat
import com.example.chatstoreservice.chat.repository.ChatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ChatService(private val chatRepository: ChatRepository) {

    @Transactional
    fun createChat(chat: Chat) {
        chatRepository.save(chat)
    }

    @Transactional
    fun updateChat(chat: Chat) {
        val findChat = chatRepository.findByChatId(chat.chatId).orElseThrow { IllegalArgumentException() }
        findChat.updateChatMessage(chat.message)
    }

    @Transactional
    fun deleteChat(chat: Chat) {
        val findChat = chatRepository.findByChatId(chat.chatId).orElseThrow { IllegalArgumentException() }
        chatRepository.delete(findChat)
    }
}
