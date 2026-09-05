package com.example.chatservice.kafka.producer;

import com.example.chatservice.message.entity.ChatMessage;
import com.example.chatservice.message.entity.SubscribeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private static final String TOPIC = "topic-chat-broadcast";
    private static final String ROOM_CREATED_TOPIC = "topic-chat-room-created";

    public void sendMessage(String key, ChatMessage message) {
        kafkaTemplate.send(TOPIC, key, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message sent successfully: {}", result.getRecordMetadata().topic() + " / " + message);
                    } else {
                        log.info("Message sent failed: {}", ex.getMessage());
                    }
                });
    }

    /** 방 생성을 ws-service 에 알린다. roomId 만 실어 보내고, 나머지 필드는 클라이언트가 방 목록을 다시 조회해 채운다. */
    public void sendRoomCreatedMessage(String roomId) {
        ChatMessage message = new ChatMessage(SubscribeType.ROOM_CREATED, roomId, null);
        kafkaTemplate.send(ROOM_CREATED_TOPIC, roomId, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Room created event sent successfully: {}", result.getRecordMetadata().topic() + " / " + message);
                    } else {
                        log.info("Room created event sent failed: {}", ex.getMessage());
                    }
                });
    }
}
