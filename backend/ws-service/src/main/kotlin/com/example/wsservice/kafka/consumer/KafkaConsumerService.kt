package com.example.wsservice.kafka.consumer

import com.example.wsservice.chat.dto.ChatMessage
import com.example.wsservice.chat.service.ChatRoomService
import com.example.wsservice.chat.service.ChatService
import com.example.wsservice.handler.WebSocketHandler
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import java.io.IOException

@Service
class KafkaConsumerService(
    private val chatService: ChatService,
    private val chatRoomService: ChatRoomService,
    private val objectMapper: ObjectMapper,
    private val webSocketHandler: WebSocketHandler,
) {

    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    @KafkaListener(topics = ["topic-chat-broadcast"], containerFactory = "kafkaListenerContainerFactory")
    fun receive(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            val chatMessage = consumerRecord.value()
            val roomId = chatMessage.roomId ?: return
            val chatRoomDto = chatRoomService.getChatRoom(roomId)
            val chatDto = chatService.getChat(chatMessage.chatId ?: return)
            // 차단 판단은 발신 인스턴스가 이미 했다. 여기서는 메시지에 실려 온 값만 쓴다
            // (다른 ws 인스턴스에 붙어 있는 세션도 같은 결과가 나오도록).
            val excludeUserIds = chatMessage.excludeUserIds.orEmpty().toSet()
            if (chatRoomDto.roomId == chatDto.roomId) {
                val textMessage = TextMessage(objectMapper.writeValueAsString(chatDto))
                chatRoomDto.members.orEmpty().forEach { member ->
                    val userId = member.userId ?: return@forEach
                    if (excludeUserIds.contains(userId)) return@forEach
                    webSocketHandler.clients[userId]?.let { s ->
                        try {
                            s.sendMessage(textMessage)
                        } catch (e: IOException) {
                            log.error("failed to push chat to {}", userId, e)
                        }
                    }
                }
            }
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }

    @KafkaListener(topics = ["topic-chat-read"], containerFactory = "kafkaListenerContainerFactory")
    fun receiveRead(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            val chatMessage = consumerRecord.value()
            val chatRoomDto = chatRoomService.getChatRoom(chatMessage.roomId ?: return)
            // 채팅 브로드캐스트와 달리 chatId 로 채팅을 조회하지 않는다. 읽음에는 저장된 채팅이 없다.
            val payload = linkedMapOf<String, Any?>(
                "type" to "READ",
                "roomId" to chatMessage.roomId,
                "userId" to chatMessage.userId,
                "lastReadChatId" to chatMessage.chatId,
            )
            broadcast(chatRoomDto.members.orEmpty().mapNotNull { it.userId }, TextMessage(objectMapper.writeValueAsString(payload)), "read")
        } catch (e: Exception) {
            log.error("read broadcast failed", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }

    /** 반응 집계를 방 인원 전부에게(반응자 본인 포함 — 앱의 낙관적 표시를 확정한다). */
    @KafkaListener(topics = ["topic-chat-reaction"], containerFactory = "kafkaListenerContainerFactory")
    fun receiveReaction(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            val chatMessage = consumerRecord.value()
            val chatRoomDto = chatRoomService.getChatRoom(chatMessage.roomId ?: return)
            val payload = linkedMapOf<String, Any?>(
                "type" to "REACTION",
                "roomId" to chatMessage.roomId,
                "chatId" to chatMessage.chatId,
                "reactions" to chatMessage.reactions.orEmpty(),
            )
            broadcast(chatRoomDto.members.orEmpty().mapNotNull { it.userId }, TextMessage(objectMapper.writeValueAsString(payload)), "reaction")
        } catch (e: Exception) {
            log.error("reaction broadcast failed", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }

    /**
     * 방 생성을 멤버들에게 알린다. chat-service 는 커밋 이후에만 이 이벤트를 보내므로
     * getChatRoom 조회는 항상 성공한다. 프레임은 roomId 만 싣는다 - 안드로이드는
     * 이를 받으면 방 목록을 REST 로 다시 조회해 나머지 필드를 채운다.
     */
    @KafkaListener(topics = ["topic-chat-room-created"], containerFactory = "kafkaListenerContainerFactory")
    fun receiveRoomCreated(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            val chatMessage = consumerRecord.value()
            val chatRoomDto = chatRoomService.getChatRoom(chatMessage.roomId ?: return)
            val payload = linkedMapOf<String, Any?>("type" to "ROOM_CREATED", "roomId" to chatMessage.roomId)
            broadcast(chatRoomDto.members.orEmpty().mapNotNull { it.userId }, TextMessage(objectMapper.writeValueAsString(payload)), "room-created")
        } catch (e: Exception) {
            log.error("room-created broadcast failed", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }

    /** 접속해 있는 멤버 세션에만 보낸다. 세션이 없는 멤버는 건너뛴다. */
    private fun broadcast(userIds: List<String>, textMessage: TextMessage, what: String) {
        userIds.forEach { userId ->
            webSocketHandler.clients[userId]?.let { s ->
                try {
                    s.sendMessage(textMessage)
                } catch (e: IOException) {
                    log.error("failed to push {} to {}", what, userId, e)
                }
            }
        }
    }
}
