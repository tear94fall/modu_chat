package com.example.chatstoreservice.chat.repository

import com.example.chatstoreservice.chat.entity.Chat
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ChatRepository : MongoRepository<Chat, String> {

    fun findByChatId(chatId: Long?): Optional<Chat>
}
