package com.example.wsservice.handler

import com.example.wsservice.block.BlockRelationCache
import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.chat.dto.ChatRoomDto
import com.example.wsservice.chat.dto.ReactionEmojiText
import com.example.wsservice.chat.dto.ReactionResultDto
import com.example.wsservice.chat.dto.SubscribeType
import com.example.wsservice.chat.service.ChatRoomService
import com.example.wsservice.chat.service.ChatService
import com.example.wsservice.fcm.dto.FcmMessageDto
import com.example.wsservice.fcm.dto.FcmUserMessageDto
import com.example.wsservice.fcm.service.FcmService
import com.example.wsservice.kafka.producer.KafkaProducerService
import com.example.wsservice.util.TimeUtil
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketHandler(
    private val objectMapper: ObjectMapper,
    private val chatService: ChatService,
    private val chatRoomService: ChatRoomService,
    private val fcmService: FcmService,
    private val kafkaProducerService: KafkaProducerService,
    private val blockRelationCache: BlockRelationCache,
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(WebSocketHandler::class.java)

    /** userId → 세션. 인스턴스 전체가 하나를 공유한다(옛 자바의 static 과 같다). 테스트가 같은 패키지에서 부른다. */
    val clients: ConcurrentHashMap<String, WebSocketSession> get() = CLIENTS

    public override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = objectMapper.readTree(message.payload)
        // READ 는 채팅 파싱보다 먼저 갈라낸다. 저장할 채팅이 없고,
        // 아래 updateLastChat 경로는 chatType 이 유효할 때만 안전하다.
        val type = payload.get("type")?.asText()
        if (type == "READ") {
            handleReadMessage(payload)
            return
        }
        if (type == "REACTION") {
            handleReactionMessage(payload)
            return
        }

        val recvChatDto = objectMapper.treeToValue(payload, ChatDto::class.java)
        val roomId = recvChatDto.roomId ?: return
        val chatRoomDto = chatRoomService.getChatRoom(roomId)
        if (chatRoomDto.checkChatRoomMember(recvChatDto.sender)) return

        recvChatDto.chatTime = TimeUtil.calculateTime(recvChatDto.chatTime ?: "")
        val chatId = chatService.saveChat(recvChatDto)
        chatRoomDto.updateLastChat(chatId.toString(), recvChatDto)
        val updateChatRoomDto = chatRoomService.updateChatRoom(roomId, chatRoomDto)

        // 차단은 전달·푸시에서만 뺀다 - 저장과 lastChat 갱신은 그대로라 발신자에게는 정상으로 보이고,
        // 차단 해제 후에는 그 기간 메시지가 이력에서 다시 보인다.
        val excludeUserIds = resolveExcludeUserIds(chatRoomDto, recvChatDto.sender)
        val chatMessage = ChatMessage(
            type = SubscribeType.BROAD_CAST,
            roomId = updateChatRoomDto.roomId,
            chatId = chatId.toString(),
            excludeUserIds = excludeUserIds.ifEmpty { null },
        )
        kafkaProducerService.sendMessage(chatMessage.roomId ?: roomId, chatMessage)

        // 1:1 방의 푸시 대상은 상대 한 명뿐이다. 그 상대가 제외 대상이면 보낼 곳이 없다.
        if (excludeUserIds.isNotEmpty()) return
        // 푸시는 참여자 목록이 있는 조회 DTO 로 만든다. updateChatRoom 응답은 ModelMapper 매핑이라 members 가 비어
        // senderName/memberCount 를 채울 수 없다. chatRoomDto 는 위 updateLastChat 로 마지막 메시지도 이미 반영돼 있다.
        try {
            fcmService.sendFcmMessage(FcmMessageDto(chatRoomDto, recvChatDto))
        } catch (e: RuntimeException) {
            // 푸시는 부가 기능이다. 여기서 예외가 새어 나가면 ExceptionWebSocketHandlerDecorator 가
            // 발신자 세션을 닫아 버려, 이미 저장·브로드캐스트된 메시지의 에코를 발신자만 못 받는다.
            log.warn("[ws] push failed for room {} chat {}: {}", chatRoomDto.roomId, chatId, e.message)
        }
    }

    /**
     * 1:1 방(멤버 정확히 2명)에서 상대가 발신자를 차단했으면 상대 userId 를 돌려준다.
     * 단체방은 서버가 그대로 두고 앱 필터에 맡긴다(카카오톡과 같은 동작).
     */
    private fun resolveExcludeUserIds(chatRoomDto: ChatRoomDto, sender: String?): List<String> {
        val members = chatRoomDto.members ?: return emptyList()
        if (members.size != 2 || sender == null) return emptyList()
        val other = members.mapNotNull { it.userId }.firstOrNull { it != sender } ?: return emptyList()
        val blockedBy = blockRelationCache.blockedBy(sender)
        return if (blockedBy.contains(other)) listOf(other) else emptyList()
    }

    /**
     * 읽음 커서를 올리고 방 인원에게 알린다.
     * 커서는 항상 방의 lastChatId 로 점프한다 — chat-service 의 updateLastReadChat 과 같은 값이라
     * 브로드캐스트에 실어 보내는 숫자와 DB 값이 어긋나지 않는다.
     */
    private fun handleReadMessage(payload: JsonNode) {
        val roomId = payload.get("roomId")?.asText()
        val userId = payload.get("sender")?.asText()
        if (roomId == null || userId == null) {
            log.warn("[ws] malformed READ frame: {}", payload)
            return
        }
        val chatRoomDto = chatRoomService.getChatRoom(roomId)
        if (chatRoomDto.checkChatRoomMember(userId)) return

        chatRoomService.updateLastReadChat(roomId, userId)
        // 메시지가 하나도 없는 방은 lastChatId 가 빈 문자열이다. 그대로 실어 보내면
        // 클라이언트가 숫자로 읽지 못해 프레임을 버린다 — 0 으로 내린다.
        val cursor = chatRoomDto.lastChatId?.takeIf { it.isNotBlank() } ?: "0"
        kafkaProducerService.sendReadMessage(roomId, ChatMessage(type = SubscribeType.READ, roomId = roomId, chatId = cursor, userId = userId))
    }

    /**
     * 반응 프레임 `{type:"REACTION", roomId, chatId, sender, emoji}`. chat-service 가 토글하고(같은 이모지면 취소),
     * 결과 집계를 방 인원에게 브로드캐스트한다. 남겨진 경우에만 메시지 작성자에게 푸시 한 통.
     * chat-service 가 거부하면(내 메시지·모르는 이모지) 프레임을 버리고 로그만 남긴다 — 세션은 닫지 않는다.
     */
    private fun handleReactionMessage(payload: JsonNode) {
        val roomId = payload.get("roomId")?.asText()
        val chatId = payload.get("chatId")?.asText()
        val userId = payload.get("sender")?.asText()
        val emoji = payload.get("emoji")?.asText()
        if (roomId == null || chatId == null || userId == null || emoji == null) {
            log.warn("[ws] malformed REACTION frame: {}", payload)
            return
        }
        val chatRoomDto = chatRoomService.getChatRoom(roomId)
        if (chatRoomDto.checkChatRoomMember(userId)) return

        val result = try {
            chatService.react(roomId, chatId, userId, emoji)
        } catch (e: RuntimeException) {
            log.warn("[ws] reaction rejected for room {} chat {} by {}: {}", roomId, chatId, userId, e.message)
            return
        }
        kafkaProducerService.sendReactionMessage(roomId, ChatMessage.reaction(result, userId))

        val author = result.authorUserId
        if (!result.added || author == null || author == userId) return
        try {
            fcmService.sendUserMessage(reactionPush(chatRoomDto, result, userId))
        } catch (e: RuntimeException) {
            log.warn("[ws] reaction push failed for room {} chat {}: {}", roomId, chatId, e.message)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        super.afterConnectionEstablished(session)
        val userId = resolveUserId(session)
        if (userId == null) {
            log.warn("[ws] userId header missing, closing session {}", session.id)
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        val previous = CLIENTS.put(userId, session)
        if (previous != null && previous.isOpen) {
            log.info("[ws] closing stale session for {}", userId)
            previous.close(CloseStatus.SESSION_NOT_RELIABLE)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        super.afterConnectionClosed(session, status)
        val userId = resolveUserId(session) ?: return
        // 2-arg remove: 값이 일치할 때만 제거해 재연결 레이스를 막는다
        CLIENTS.remove(userId, session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.error("[ws] transport error on session {}", session.id, exception)
    }

    private fun resolveUserId(session: WebSocketSession): String? = session.handshakeHeaders["userId"]?.firstOrNull()

    companion object {
        private val CLIENTS = ConcurrentHashMap<String, WebSocketSession>()

        /** "준섭님이 👍 반응을 남겼습니다". 앱은 채팅 푸시와 같은 data 모양을 기대한다(roomId·sender·senderName·memberCount·type). */
        @JvmStatic
        fun reactionPush(chatRoomDto: ChatRoomDto, result: ReactionResultDto, reactorUserId: String): FcmUserMessageDto {
            val members = chatRoomDto.members.orEmpty()
            val reactorName = members.firstOrNull { reactorUserId == it.userId }?.username ?: ""
            val body = reactorName + "님이 " + ReactionEmojiText.of(result.emoji) + " 반응을 남겼습니다"
            val data = HashMap<String, String>()
            data["roomId"] = chatRoomDto.roomId ?: ""
            data["sender"] = reactorUserId
            data["senderName"] = reactorName
            data["memberCount"] = members.size.toString()
            data["type"] = "1"
            data["kind"] = "REACTION"
            data["chatId"] = result.chatId.toString()
            return FcmUserMessageDto(result.authorUserId, chatRoomDto.roomName, body, data)
        }
    }
}
