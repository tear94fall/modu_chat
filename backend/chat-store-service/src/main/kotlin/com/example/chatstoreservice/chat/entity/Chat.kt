package com.example.chatstoreservice.chat.entity

import com.example.chatstoreservice.chat.dto.ChatDto
import com.example.chatstoreservice.common.util.TimeUtil
import com.example.chatstoreservice.message.ChatModel
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/** MySQL chat 행의 Mongo 미러. Debezium CDC 로 채워진다. */
@Document(collection = "chat")
class Chat protected constructor() {

    @Id
    var id: String? = null
        protected set
    var chatId: Long? = null
        protected set
    var chatType: Int = 0
        protected set
    var roomId: String? = null
        protected set
    var sender: String? = null
        protected set
    var message: String? = null
        protected set
    var chatTime: String? = null
        protected set
    var chatRoomId: Long? = null
        protected set

    @CreatedDate
    var createdDate: LocalDateTime? = null
        protected set

    @LastModifiedDate
    var updatedDate: LocalDateTime? = null
        protected set

    fun updateChatMessage(message: String?) {
        this.message = message
    }

    constructor(chatDto: ChatDto) : this() {
        chatType = chatDto.chatType
        roomId = chatDto.roomId
        sender = chatDto.sender
        message = chatDto.message
        chatTime = chatDto.chatTime
    }

    constructor(chatModel: ChatModel) : this() {
        chatId = chatModel.chatId
        chatType = chatModel.chatType
        roomId = chatModel.roomId
        sender = chatModel.sender
        message = chatModel.message
        chatTime = chatModel.chatTime
        chatRoomId = chatModel.chatRoomId
        createdDate = TimeUtil.converDateToLocalDateTime(chatModel.createdDate)
        updatedDate = TimeUtil.converDateToLocalDateTime(chatModel.updatedDate)
    }
}
