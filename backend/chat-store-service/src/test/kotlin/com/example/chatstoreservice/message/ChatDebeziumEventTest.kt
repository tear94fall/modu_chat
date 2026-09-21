package com.example.chatstoreservice.message

import com.example.chatstoreservice.chat.entity.Chat
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Kotlin 모듈이 없는 ObjectMapper(KafkaConsumerConfig 와 같은 설정)로 Debezium 이벤트가 읽히는지 본다. */
class ChatDebeziumEventTest {

    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun snakeCaseRowIsMappedAndUnknownFieldsIgnored() {
        val json = """
            {"schema":{"type":"struct"},"payload":{"op":"c","before":null,
             "after":{"chat_id":77,"chat_type":1,"room_id":"room-1","sender":"u1","message":"hi","chat_time":"2026-09-21 10:00:00",
                      "chat_room_id":46,"created_date":1789800000000,"updated_date":null},"source":{"db":"modu-chat"}}}
        """.trimIndent()

        val event = mapper.readValue(json, ChatListener::class.java)
        val after = event.payload?.after
        assertNotNull(after)
        assertEquals("c", event.payload?.op)
        assertEquals(77L, after.chatId)
        assertEquals("room-1", after.roomId)
        assertEquals(46L, after.chatRoomId)

        val chat = Chat(after)
        assertEquals(77L, chat.chatId)
        assertEquals("hi", chat.message)
        assertNotNull(chat.createdDate)
        assertNotNull(chat.updatedDate)
    }
}
