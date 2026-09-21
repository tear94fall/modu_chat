package com.example.wsservice.handler

import com.example.wsservice.block.BlockRelationCache
import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.chat.dto.ChatRoomDto
import com.example.wsservice.chat.dto.ReactionResultDto
import com.example.wsservice.chat.dto.ReactionSummaryDto
import com.example.wsservice.chat.dto.SubscribeType
import com.example.wsservice.chat.service.ChatRoomService
import com.example.wsservice.chat.service.ChatService
import com.example.wsservice.fcm.dto.FcmMessageDto
import com.example.wsservice.fcm.dto.FcmUserMessageDto
import com.example.wsservice.fcm.service.FcmService
import com.example.wsservice.kafka.producer.KafkaProducerService
import com.example.wsservice.member.dto.MemberDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.URI

class WebSocketHandlerTest {

    private lateinit var handler: WebSocketHandler
    private lateinit var objectMapper: ObjectMapper
    private lateinit var chatService: ChatService
    private lateinit var chatRoomService: ChatRoomService
    private lateinit var fcmService: FcmService
    private lateinit var kafkaProducerService: KafkaProducerService
    private lateinit var blockRelationCache: BlockRelationCache

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        chatService = mock()
        chatRoomService = mock()
        fcmService = mock()
        kafkaProducerService = mock()
        blockRelationCache = mock()
        handler = WebSocketHandler(objectMapper, chatService, chatRoomService, fcmService, kafkaProducerService, blockRelationCache)
        handler.clients.clear() // CLIENTS 가 공유 상태라 테스트 간 격리 필요
    }

    private fun session(userId: String, open: Boolean): WebSocketSession {
        val s = mock<WebSocketSession>()
        whenever(s.uri).thenReturn(URI.create("ws://localhost:8090/modu-chat"))
        val headers = HttpHeaders()
        headers.add("userId", userId)
        whenever(s.handshakeHeaders).thenReturn(headers)
        whenever(s.isOpen).thenReturn(open)
        return s
    }

    private fun member(userId: String) = MemberDto(userId = userId, username = userId)

    private fun room(roomId: String, vararg userIds: String) = ChatRoomDto(roomId = roomId, roomName = roomId, members = userIds.map { member(it) })

    private fun chatFrame(roomId: String, sender: String): TextMessage =
        TextMessage(objectMapper.writeValueAsString(ChatDto(message = "hello", roomId = roomId, sender = sender, chatTime = "2026-09-13 10:00:00", chatType = 1)))

    private fun capturedBroadcast(roomId: String): ChatMessage {
        val captor = argumentCaptor<ChatMessage>()
        verify(kafkaProducerService).sendMessage(eq(roomId), captor.capture())
        return captor.firstValue
    }

    @Test
    @DisplayName("roomId 없는 경로로 접속해도 세션이 등록된다")
    fun registersSessionWithoutRoomIdInPath() {
        val s = session("user-a", true)

        handler.afterConnectionEstablished(s)

        assertThat(handler.clients["user-a"]).isSameAs(s)
    }

    @Test
    @DisplayName("같은 유저가 재연결하면 이전 세션을 닫는다")
    fun closesPreviousSessionOnReconnect() {
        val first = session("user-a", true)
        val second = session("user-a", true)

        handler.afterConnectionEstablished(first)
        handler.afterConnectionEstablished(second)

        verify(first).close(CloseStatus.SESSION_NOT_RELIABLE)
        assertThat(handler.clients["user-a"]).isSameAs(second)
    }

    @Test
    @DisplayName("옛 세션의 close 이벤트가 새 세션을 제거하지 않는다")
    fun staleCloseDoesNotEvictNewSession() {
        val first = session("user-a", true)
        val second = session("user-a", true)

        handler.afterConnectionEstablished(first)
        handler.afterConnectionEstablished(second)
        handler.afterConnectionClosed(first, CloseStatus.SESSION_NOT_RELIABLE)

        assertThat(handler.clients["user-a"]).isSameAs(second)
    }

    @Test
    @DisplayName("자신의 세션이 닫히면 제거된다")
    fun removesOwnSessionOnClose() {
        val s = session("user-a", true)

        handler.afterConnectionEstablished(s)
        handler.afterConnectionClosed(s, CloseStatus.NORMAL)

        assertThat(handler.clients).doesNotContainKey("user-a")
    }

    @Test
    @DisplayName("이미 닫힌 이전 세션에는 close 를 호출하지 않는다")
    fun doesNotCloseAlreadyClosedSession() {
        val first = session("user-a", false)
        val second = session("user-a", true)

        handler.afterConnectionEstablished(first)
        handler.afterConnectionEstablished(second)

        verify(first, never()).close(CloseStatus.SESSION_NOT_RELIABLE)
    }

    @Test
    @DisplayName("1:1 방에서 상대가 나를 차단했으면 excludeUserIds 에 상대가 실리고 푸시는 보내지 않는다")
    fun oneToOneBlocked_excludesOtherAndSkipsFcm() {
        val chatRoomDto = room("room-1", "user-a", "user-b")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto)
        whenever(chatService.saveChat(any())).thenReturn(7L)
        whenever(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto)
        whenever(blockRelationCache.blockedBy("user-a")).thenReturn(setOf("user-b"))

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"))

        val sent = capturedBroadcast("room-1")
        assertThat(sent.type).isEqualTo(SubscribeType.BROAD_CAST)
        assertThat(sent.chatId).isEqualTo("7")
        assertThat(sent.excludeUserIds).containsExactly("user-b")

        // 발신자에게는 정상으로 보여야 하므로 저장·lastChat 갱신은 그대로 한다
        verify(chatService).saveChat(any())
        verify(chatRoomService).updateChatRoom(eq("room-1"), any())
        assertThat(chatRoomDto.lastChatId).isEqualTo("7")

        verify(fcmService, never()).sendFcmMessage(any())
    }

    @Test
    @DisplayName("1:1 방에서 차단이 없으면 제외 없이 기존대로 푸시까지 간다")
    fun oneToOneNotBlocked_behavesAsBefore() {
        val chatRoomDto = room("room-1", "user-a", "user-b")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto)
        whenever(chatService.saveChat(any())).thenReturn(8L)
        whenever(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto)
        whenever(blockRelationCache.blockedBy("user-a")).thenReturn(setOf())

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"))

        assertThat(capturedBroadcast("room-1").excludeUserIds).isNullOrEmpty()
        verify(fcmService).sendFcmMessage(any<FcmMessageDto>())
    }

    @Test
    @DisplayName("푸시 전송이 실패해도 세션을 닫지 않는다 - 저장과 브로드캐스트는 이미 끝났으므로 예외를 삼킨다")
    fun fcmFailure_doesNotPropagate() {
        val chatRoomDto = room("room-1", "user-a", "user-b")
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto)
        whenever(chatService.saveChat(any())).thenReturn(9L)
        whenever(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto)
        whenever(blockRelationCache.blockedBy("user-a")).thenReturn(setOf())
        doThrow(RuntimeException("push-service 500")).whenever(fcmService).sendFcmMessage(any<FcmMessageDto>())

        val session = session("user-a", true)

        // 예외가 새어 나가면 ExceptionWebSocketHandlerDecorator 가 발신자 세션을 닫고 에코가 유실된다
        handler.handleTextMessage(session, chatFrame("room-1", "user-a"))

        assertThat(capturedBroadcast("room-1").chatId).isEqualTo("9")
        verify(session, never()).close(any<CloseStatus>())
    }

    @Test
    @DisplayName("단체방은 차단이 있어도 제외하지 않고 푸시도 그대로 보낸다")
    fun groupRoomBlocked_doesNotExclude() {
        val chatRoomDto = room("room-2", "user-a", "user-b", "user-c")
        whenever(chatRoomService.getChatRoom("room-2")).thenReturn(chatRoomDto)
        whenever(chatService.saveChat(any())).thenReturn(9L)
        whenever(chatRoomService.updateChatRoom(eq("room-2"), any())).thenReturn(chatRoomDto)

        handler.handleTextMessage(session("user-a", true), chatFrame("room-2", "user-a"))

        assertThat(capturedBroadcast("room-2").excludeUserIds).isNullOrEmpty()
        verify(fcmService).sendFcmMessage(any<FcmMessageDto>())
        verify(blockRelationCache, never()).blockedBy(any())
    }

    @Test
    @DisplayName("READ 프레임은 차단 조회 없이 기존 흐름 그대로다")
    fun readFrame_isUnchanged() {
        val chatRoomDto = room("room-1", "user-a", "user-b")
        chatRoomDto.lastChatId = "11"
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto)

        handler.handleTextMessage(session("user-b", true), TextMessage("{\"type\":\"READ\",\"roomId\":\"room-1\",\"sender\":\"user-b\"}"))

        val captor = argumentCaptor<ChatMessage>()
        verify(kafkaProducerService).sendReadMessage(eq("room-1"), captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(SubscribeType.READ)
        assertThat(captor.firstValue.chatId).isEqualTo("11")
        assertThat(captor.firstValue.excludeUserIds).isNull()

        verify(blockRelationCache, never()).blockedBy(any())
        verify(kafkaProducerService, never()).sendMessage(any(), any())
    }

    @Test
    @DisplayName("차단 대상 목록이 붙어도 방 멤버가 아니면 아무것도 하지 않는다")
    fun nonMemberSender_isIgnored() {
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-b", "user-c"))

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"))

        verify(chatService, never()).saveChat(any())
        verify(kafkaProducerService, never()).sendMessage(any(), any())
        verify(fcmService, never()).sendFcmMessage(any())
    }

    @Test
    @DisplayName("excludeUserIds 가 없는 ChatMessage 는 기존 JSON 모양 그대로 직렬화된다")
    fun chatMessageWithoutExclusion_keepsLegacyJsonShape() {
        val json = objectMapper.writeValueAsString(ChatMessage(type = SubscribeType.BROAD_CAST, roomId = "room-1", chatId = "7"))

        assertThat(json).isEqualTo("{\"type\":\"BROAD_CAST\",\"roomId\":\"room-1\",\"chatId\":\"7\"}")

        // 필드가 없는 옛 메시지도 그대로 읽힌다
        val legacy = objectMapper.readValue("{\"type\":\"BROAD_CAST\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"userId\":null}", ChatMessage::class.java)
        assertThat(legacy.excludeUserIds).isNull()

        val withExclusion = objectMapper.readValue(
            objectMapper.writeValueAsString(ChatMessage(type = SubscribeType.BROAD_CAST, roomId = "room-1", chatId = "7", excludeUserIds = listOf("user-b"))),
            ChatMessage::class.java,
        )
        assertThat(withExclusion.excludeUserIds).containsExactly("user-b")
    }

    @Test
    @DisplayName("REACTION 프레임: chat-service 결과를 반응 토픽에 싣고, 남겨졌으면 작성자에게만 푸시한다")
    fun reactionFrame_broadcastsAndPushesAuthor() {
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"))
        val result = ReactionResultDto(chatId = 7L, roomId = "room-1", authorUserId = "user-a", added = true, emoji = "LIKE",
            reactions = listOf(ReactionSummaryDto("LIKE", 1, listOf("user-b"))))
        whenever(chatService.react("room-1", "7", "user-b", "LIKE")).thenReturn(result)

        handler.handleTextMessage(session("user-b", true),
            TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-b\",\"emoji\":\"LIKE\"}"))

        val captor = argumentCaptor<ChatMessage>()
        verify(kafkaProducerService).sendReactionMessage(eq("room-1"), captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(SubscribeType.REACTION)
        assertThat(captor.firstValue.chatId).isEqualTo("7")
        assertThat(captor.firstValue.userId).isEqualTo("user-b")
        assertThat(captor.firstValue.reactions).hasSize(1)

        val push = argumentCaptor<FcmUserMessageDto>()
        verify(fcmService).sendUserMessage(push.capture())
        assertThat(push.firstValue.userId).isEqualTo("user-a")
        assertThat(push.firstValue.body).isEqualTo("user-b님이 👍 반응을 남겼습니다")
        assertThat(push.firstValue.data).containsEntry("roomId", "room-1").containsEntry("sender", "user-b").containsEntry("kind", "REACTION")
        verify(kafkaProducerService, never()).sendMessage(any(), any())
    }

    @Test
    @DisplayName("반응 취소는 브로드캐스트만 하고 푸시하지 않는다")
    fun reactionRemoval_doesNotPush() {
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"))
        whenever(chatService.react("room-1", "7", "user-b", "LIKE"))
            .thenReturn(ReactionResultDto(chatId = 7L, roomId = "room-1", authorUserId = "user-a", added = false, reactions = listOf()))

        handler.handleTextMessage(session("user-b", true),
            TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-b\",\"emoji\":\"LIKE\"}"))

        verify(kafkaProducerService).sendReactionMessage(eq("room-1"), any())
        verify(fcmService, never()).sendUserMessage(any())
    }

    @Test
    @DisplayName("chat-service 가 거부하면(내 메시지 등) 아무것도 보내지 않고 세션도 살아 있다")
    fun reactionRejected_isDropped() {
        whenever(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"))
        whenever(chatService.react("room-1", "7", "user-a", "LIKE")).thenThrow(RuntimeException("400 own chat"))

        handler.handleTextMessage(session("user-a", true),
            TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-a\",\"emoji\":\"LIKE\"}"))

        verify(kafkaProducerService, never()).sendReactionMessage(any(), any())
        verify(fcmService, never()).sendUserMessage(any())
    }
}
