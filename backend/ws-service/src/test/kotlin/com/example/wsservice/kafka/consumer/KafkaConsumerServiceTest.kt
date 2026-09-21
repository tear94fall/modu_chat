package com.example.wsservice.kafka.consumer

import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.chat.dto.ChatRoomDto
import com.example.wsservice.chat.dto.ReactionSummaryDto
import com.example.wsservice.chat.dto.SubscribeType
import com.example.wsservice.chat.service.ChatRoomService
import com.example.wsservice.chat.service.ChatService
import com.example.wsservice.handler.WebSocketHandler
import com.example.wsservice.member.dto.MemberDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.support.Acknowledgment
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

class KafkaConsumerServiceTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var chatService: ChatService
    private lateinit var chatRoomService: ChatRoomService
    private lateinit var webSocketHandler: WebSocketHandler
    private lateinit var clients: ConcurrentHashMap<String, WebSocketSession>
    private lateinit var kafkaConsumerService: KafkaConsumerService
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        chatService = mock()
        chatRoomService = mock()
        webSocketHandler = mock()
        clients = ConcurrentHashMap()
        whenever(webSocketHandler.clients).thenReturn(clients)
        acknowledgment = mock()
        objectMapper = ObjectMapper()
        kafkaConsumerService = KafkaConsumerService(chatService, chatRoomService, objectMapper, webSocketHandler)
    }

    private fun member(userId: String) = MemberDto(userId = userId)

    private fun roomWithMembers(roomId: String, vararg userIds: String) = ChatRoomDto(roomId = roomId, members = userIds.map { member(it) })

    private fun chat(roomId: String) = ChatDto(id = 7L, roomId = roomId, sender = "user-a", message = "hello", chatType = 1, chatTime = "2026-09-13 10:00:00")

    private fun broadcastRecord(roomId: String, excludeUserIds: List<String>?) = ConsumerRecord(
        BROADCAST_TOPIC, 0, 0L, roomId, ChatMessage(type = SubscribeType.BROAD_CAST, roomId = roomId, chatId = "7", excludeUserIds = excludeUserIds),
    )

    @Test
    @DisplayName("excludeUserIds 에 든 userId 의 세션에는 브로드캐스트하지 않는다")
    fun receive_skipsExcludedSessions() {
        val sender = mock<WebSocketSession>()
        val blocked = mock<WebSocketSession>()
        clients["user-a"] = sender
        clients["user-b"] = blocked

        val chatDto = chat("room-1")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"))
        whenever(chatService.getChat("7")).thenReturn(chatDto)

        kafkaConsumerService.receive(broadcastRecord("room-1", listOf("user-b")), acknowledgment)

        val expected = TextMessage(objectMapper.writeValueAsString(chatDto))
        verify(sender).sendMessage(expected)
        verify(blocked, never()).sendMessage(any<TextMessage>())
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("excludeUserIds 가 비어 있으면 멤버 전원에게 보낸다")
    fun receive_withoutExclusion_sendsToEveryone() {
        val a = mock<WebSocketSession>()
        val b = mock<WebSocketSession>()
        clients["user-a"] = a
        clients["user-b"] = b

        val chatDto = chat("room-1")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"))
        whenever(chatService.getChat("7")).thenReturn(chatDto)

        kafkaConsumerService.receive(broadcastRecord("room-1", null), acknowledgment)

        val expected = TextMessage(objectMapper.writeValueAsString(chatDto))
        verify(a).sendMessage(expected)
        verify(b).sendMessage(expected)
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("필드가 없는 옛 메시지(excludeUserIds=null)도 그대로 동작한다")
    fun receive_legacyMessageWithoutField_stillBroadcasts() {
        val a = mock<WebSocketSession>()
        clients["user-a"] = a

        val chatDto = chat("room-1")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"))
        whenever(chatService.getChat("7")).thenReturn(chatDto)

        val record = ConsumerRecord(BROADCAST_TOPIC, 0, 0L, "room-1", ChatMessage(type = SubscribeType.BROAD_CAST, roomId = "room-1", chatId = "7"))

        kafkaConsumerService.receive(record, acknowledgment)

        verify(a).sendMessage(TextMessage(objectMapper.writeValueAsString(chatDto)))
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("살아있는 세션에만 ROOM_CREATED 프레임을 보내고 없는 세션은 건너뛴다")
    fun receiveRoomCreated_sendsOnlyToPresentSessions() {
        val present = mock<WebSocketSession>()
        clients["user-a"] = present
        // user-b 는 접속해 있지 않다 - clients 맵에 없다.

        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"))

        val record = ConsumerRecord(TOPIC, 0, 0L, "room-1", ChatMessage(type = SubscribeType.ROOM_CREATED, roomId = "room-1"))

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment)

        verify(present).sendMessage(TextMessage("{\"type\":\"ROOM_CREATED\",\"roomId\":\"room-1\"}"))
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("멤버 전원이 오프라인이면 아무 세션에도 보내지 않지만 ack 는 한다")
    fun receiveRoomCreated_withNoLiveSessions_acknowledgesWithoutSending() {
        whenever(chatRoomService.getChatRoom("room-2")).thenReturn(roomWithMembers("room-2", "user-c"))

        val record = ConsumerRecord(TOPIC, 0, 0L, "room-2", ChatMessage(type = SubscribeType.ROOM_CREATED, roomId = "room-2"))

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment)

        assertThat(clients).isEmpty()
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("방 조회가 실패해도 예외를 삼키고 ack 는 finally 에서 한다")
    fun receiveRoomCreated_whenRoomLookupFails_stillAcknowledges() {
        whenever(chatRoomService.getChatRoom("missing-room")).thenThrow(RuntimeException("not found"))

        val record = ConsumerRecord(TOPIC, 0, 0L, "missing-room", ChatMessage(type = SubscribeType.ROOM_CREATED, roomId = "missing-room"))

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment)

        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("반응 토픽: 집계를 방 인원 전원에게 REACTION 프레임으로 보낸다(반응자 본인 포함)")
    fun receiveReaction_sendsSummaryToEveryone() {
        val a = mock<WebSocketSession>()
        val b = mock<WebSocketSession>()
        clients["user-a"] = a
        clients["user-b"] = b
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"))
        val message = ChatMessage(type = SubscribeType.REACTION, roomId = "room-1", chatId = "7", userId = "user-b",
            authorUserId = "user-a", emoji = "LIKE", added = true, reactions = listOf(ReactionSummaryDto("LIKE", 1, listOf("user-b"))))

        kafkaConsumerService.receiveReaction(ConsumerRecord("topic-chat-reaction", 0, 0L, "room-1", message), acknowledgment)

        val captor = argumentCaptor<TextMessage>()
        verify(a).sendMessage(captor.capture())
        verify(b).sendMessage(any<TextMessage>())
        assertThat(captor.firstValue.payload).contains("\"type\":\"REACTION\"").contains("\"chatId\":\"7\"")
            .contains("\"emoji\":\"LIKE\"").contains("\"userIds\":[\"user-b\"]")
        verify(chatService, never()).getChat(any())
        verify(acknowledgment).acknowledge()
    }

    companion object {
        private const val TOPIC = "topic-chat-room-created"
        private const val BROADCAST_TOPIC = "topic-chat-broadcast"
    }
}
