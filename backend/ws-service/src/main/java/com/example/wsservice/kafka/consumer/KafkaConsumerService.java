package com.example.wsservice.kafka.consumer;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.handler.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;
    private final WebSocketHandler webSocketHandler;

    @KafkaListener(
            topics = "topic-chat-broadcast",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void receive(ConsumerRecord<String, ChatMessage> consumerRecord, Acknowledgment acknowledgment) {
        try {
            String key = consumerRecord.key();
            ChatMessage chatMessage = consumerRecord.value();

            ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(chatMessage.getRoomId());
            ChatDto chatDto = chatService.getChat(chatMessage.getChatId());

            if (chatRoomDto.getRoomId().equals(chatDto.getRoomId())) {
                String payload = objectMapper.writeValueAsString(chatDto);

                TextMessage textMessage = new TextMessage(payload);

                chatRoomDto.getMembers().forEach(member -> {
                    String userId = member.getUserId();

                    WebSocketSession s = webSocketHandler.getClients().get(userId);
                    if (s != null) {
                        try {
                            s.sendMessage(textMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    @KafkaListener(
            topics = "topic-chat-read",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void receiveRead(ConsumerRecord<String, ChatMessage> consumerRecord, Acknowledgment acknowledgment) {
        try {
            ChatMessage chatMessage = consumerRecord.value();

            ChatRoomDto chatRoomDto = chatRoomService.getChatRoom(chatMessage.getRoomId());

            // 채팅 브로드캐스트와 달리 chatId 로 채팅을 조회하지 않는다. 읽음에는 저장된 채팅이 없다.
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "READ");
            payload.put("roomId", chatMessage.getRoomId());
            payload.put("userId", chatMessage.getUserId());
            payload.put("lastReadChatId", chatMessage.getChatId());

            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(payload));

            chatRoomDto.getMembers().forEach(member -> {
                WebSocketSession s = webSocketHandler.getClients().get(member.getUserId());
                if (s != null) {
                    try {
                        s.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("failed to push read to {}", member.getUserId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("read broadcast failed", e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
