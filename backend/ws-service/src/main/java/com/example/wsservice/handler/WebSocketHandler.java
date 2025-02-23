package com.example.wsservice.handler;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.fcm.dto.FcmMessageDto;
import com.example.wsservice.fcm.service.FcmService;
import com.example.wsservice.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.wsservice.chat.dto.SubscribeType.*;
import static com.example.wsservice.util.TimeUtil.calculateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    @Value("${rabbitmq.queue.queue1.name}")
    private String queueName;

    @Value("${rabbitmq.queue.queue2.name}")
    private String wsQueueName;

    @Value("${rabbitmq.queue.queue1.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.queue2.exchange}")
    private String wsExchangeName;

    @Value("${rabbitmq.queue.routing.key.queue1}")
    private String routingKey;

    @Value("${rabbitmq.queue.routing.key.queue2}")
    private String wsRoutingKey;


    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
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

        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String userId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        if (roomId != null) {
            CLIENTS.put(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String userId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        if (CLIENTS.get(userId) != null) {
            CLIENTS.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    public ConcurrentHashMap<String, WebSocketSession> getClients() {
        return CLIENTS;
    }

    public void producerMessage(ChatMessage chatMessage) throws JsonProcessingException {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, objectMapper.writeValueAsString(chatMessage));
    }

    @Transactional
    @RabbitListener(queues = "${rabbitmq.queue.queue2.name}")
    public void consumeMessage(String message) throws JsonProcessingException {
        ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
        log.info("[consumer][message] {}", chatMessage);

        ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(chatMessage.getRoomId());
        ChatDto chatDto = chatService.getChat(chatMessage.getChatId());

        if (chatRoomDto.getRoomId().equals(chatDto.getRoomId())) {
            String payload = objectMapper.writeValueAsString(chatDto);

            TextMessage textMessage = new TextMessage(payload);

            chatRoomDto.getMembers().forEach(member -> {
                String userId = member.getUserId();
                WebSocketSession s = CLIENTS.get(userId);
                if (s != null) {
                    try {
                        s.sendMessage(textMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}