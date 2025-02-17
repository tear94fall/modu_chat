package com.example.chatservice.kafka.producer;

import com.example.chatservice.message.entity.ChatMessage;
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
}
