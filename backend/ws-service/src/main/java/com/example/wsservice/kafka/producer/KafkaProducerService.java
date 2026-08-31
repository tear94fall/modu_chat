package com.example.wsservice.kafka.producer;

import com.example.wsservice.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private static final String TOPIC = "topic-chat-save";
    private static final String READ_TOPIC = "topic-chat-read";

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

    public void sendReadMessage(String key, ChatMessage message) {
        kafkaTemplate.send(READ_TOPIC, key, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Read sent successfully: {}", result.getRecordMetadata().topic() + " / " + message);
                    } else {
                        log.info("Read sent failed: {}", ex.getMessage());
                    }
                });
    }
}
