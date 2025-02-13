package com.example.storageservice.kafka.consumer;

import com.example.storageservice.kafka.dto.ProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

@Getter
@Service
public class KafkaConsumer {

    private final CountDownLatch latch = new CountDownLatch(1);
    private ProfileDto receivedMessage;

    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void listen(ConsumerRecord<String, Object> record) {
        ObjectMapper objectMapper = new ObjectMapper();
        this.receivedMessage = objectMapper.convertValue(record.value(), ProfileDto.class);
        latch.countDown();
    }
}

