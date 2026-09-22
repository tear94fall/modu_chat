package com.example.chatstoreservice.message

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

/** Debezium 의 행 스냅샷(snake_case). 필드에 붙인 @JsonProperty 로 Kotlin 모듈 없는 ObjectMapper 도 읽는다. */
class ChatModel {
    @field:JsonProperty("chat_id")
    var chatId: Long? = null

    @field:JsonProperty("chat_type")
    var chatType: Int = 0

    @field:JsonProperty("room_id")
    var roomId: String? = null

    @field:JsonProperty("sender")
    var sender: String? = null

    @field:JsonProperty("message")
    var message: String? = null

    @field:JsonProperty("chat_time")
    var chatTime: String? = null

    @field:JsonProperty("chat_room_id")
    var chatRoomId: Long? = null

    @field:JsonProperty("created_date")
    var createdDate: Date? = null

    @field:JsonProperty("updated_date")
    var updatedDate: Date? = null

    override fun toString(): String =
        "ChatModel(chatId=$chatId, chatType=$chatType, roomId=$roomId, sender=$sender, message=$message, chatTime=$chatTime, chatRoomId=$chatRoomId, createdDate=$createdDate, updatedDate=$updatedDate)"
}
