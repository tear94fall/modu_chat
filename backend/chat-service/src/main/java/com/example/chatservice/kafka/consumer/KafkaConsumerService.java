package com.example.chatservice.kafka.consumer;

import com.example.chatservice.kafka.producer.KafkaProducerService;
import com.example.chatservice.message.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(
            topics = "topic-chat-save",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void receive(ConsumerRecord<String, ChatMessage> consumerRecord, Acknowledgment acknowledgment) {
        try {
            String key = consumerRecord.key();
            ChatMessage chatMessage = consumerRecord.value();

            kafkaProducerService.sendMessage(chatMessage.getRoomId(), chatMessage);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
