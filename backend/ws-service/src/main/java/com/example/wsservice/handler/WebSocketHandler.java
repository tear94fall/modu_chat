package com.example.wsservice.handler;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.fcm.dto.FcmMessageDto;
import com.example.wsservice.fcm.service.FcmService;
import com.example.wsservice.kafka.producer.KafkaProducerService;
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
        ChatDto recvChatDto = objectMapper.readValue(message.getPayload(), ChatDto.class);
        ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(recvChatDto.getRoomId());

        if (chatRoomDto.checkChatRoomMember(recvChatDto.getSender())) return;

        recvChatDto.setChatTime(calculateTime(recvChatDto.getChatTime()));
        Long chatId = chatService.saveChat(recvChatDto);

        chatRoomDto.updateLastChat(chatId.toString(), recvChatDto);

        ChatRoomDto updateChatRoomDto = chatRoomService.updateChatRoom(chatRoomDto.getRoomId(), chatRoomDto);

        ChatMessage chatMessage = new ChatMessage(BROAD_CAST, updateChatRoomDto.getRoomId(), chatId.toString());

        kafkaProducerService.sendMessage(chatMessage.getRoomId(), chatMessage);

        FcmMessageDto fcmMessageDto = new FcmMessageDto(updateChatRoomDto, recvChatDto);
        fcmService.sendFcmMessage(fcmMessageDto);
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
