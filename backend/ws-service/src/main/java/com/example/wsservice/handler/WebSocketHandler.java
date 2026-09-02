package com.example.wsservice.handler;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.fcm.dto.FcmMessageDto;
import com.example.wsservice.fcm.service.FcmService;
import com.example.wsservice.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.wsservice.chat.dto.SubscribeType.*;
import static com.example.wsservice.util.TimeUtil.calculateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final FcmService fcmService;
    private final KafkaProducerService kafkaProducerService;

    private static final ConcurrentHashMap<String, WebSocketSession> CLIENTS = new ConcurrentHashMap<String, WebSocketSession>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode payload = objectMapper.readTree(message.getPayload());

        // READ 는 채팅 파싱보다 먼저 갈라낸다. 저장할 채팅이 없고,
        // 아래 updateLastChat 경로는 chatType 이 유효할 때만 안전하다.
        JsonNode typeNode = payload.get("type");
        if (typeNode != null && "READ".equals(typeNode.asText())) {
            handleReadMessage(payload);
            return;
        }

        ChatDto recvChatDto = objectMapper.treeToValue(payload, ChatDto.class);
        ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(recvChatDto.getRoomId());

        if (chatRoomDto.checkChatRoomMember(recvChatDto.getSender())) return;

        recvChatDto.setChatTime(calculateTime(recvChatDto.getChatTime()));
        Long chatId = chatService.saveChat(recvChatDto);

        chatRoomDto.updateLastChat(chatId.toString(), recvChatDto);

        ChatRoomDto updateChatRoomDto = chatRoomService.updateChatRoom(chatRoomDto.getRoomId(), chatRoomDto);

        ChatMessage chatMessage = new ChatMessage(BROAD_CAST, updateChatRoomDto.getRoomId(), chatId.toString(), null);

        kafkaProducerService.sendMessage(chatMessage.getRoomId(), chatMessage);

        FcmMessageDto fcmMessageDto = new FcmMessageDto(updateChatRoomDto, recvChatDto);
        fcmService.sendFcmMessage(fcmMessageDto);
    }

    /**
     * 읽음 커서를 올리고 방 인원에게 알린다.
     * 커서는 항상 방의 lastChatId 로 점프한다 — chat-service 의 updateLastReadChat 과 같은 값이라
     * 브로드캐스트에 실어 보내는 숫자와 DB 값이 어긋나지 않는다.
     */
    private void handleReadMessage(JsonNode payload) {
        JsonNode roomIdNode = payload.get("roomId");
        JsonNode senderNode = payload.get("sender");
        if (roomIdNode == null || senderNode == null) {
            log.warn("[ws] malformed READ frame: {}", payload);
            return;
        }

        String roomId = roomIdNode.asText();
        String userId = senderNode.asText();

        ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(roomId);
        if (chatRoomDto.checkChatRoomMember(userId)) return;

        chatRoomService.updateLastReadChat(roomId, userId);

        // 메시지가 하나도 없는 방은 lastChatId 가 빈 문자열이다. 그대로 실어 보내면
        // 클라이언트가 숫자로 읽지 못해 프레임을 버린다 — 0 으로 내린다.
        String lastChatId = chatRoomDto.getLastChatId();
        String cursor = (lastChatId == null || lastChatId.isBlank()) ? "0" : lastChatId;

        ChatMessage readMessage = new ChatMessage(READ, roomId, cursor, userId);

        kafkaProducerService.sendReadMessage(roomId, readMessage);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String userId = resolveUserId(session);
        if (userId == null) {
            log.warn("[ws] userId header missing, closing session {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        WebSocketSession previous = CLIENTS.put(userId, session);
        if (previous != null && previous.isOpen()) {
            log.info("[ws] closing stale session for {}", userId);
            previous.close(CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String userId = resolveUserId(session);
        if (userId == null) return;

        // 2-arg remove: 값이 일치할 때만 제거해 재연결 레이스를 막는다
        CLIENTS.remove(userId, session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[ws] transport error on session {}", session.getId(), exception);
    }

    private String resolveUserId(WebSocketSession session) {
        List<String> values = session.getHandshakeHeaders().get("userId");
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    public ConcurrentHashMap<String, WebSocketSession> getClients() {
        return CLIENTS;
    }
}
