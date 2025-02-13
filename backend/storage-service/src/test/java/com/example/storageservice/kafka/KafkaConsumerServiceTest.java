package com.example.storageservice.kafka;

import com.example.storageservice.kafka.consumer.KafkaConsumer;
import com.example.storageservice.kafka.dto.ProfileDto;
import com.example.storageservice.kafka.dto.ProfileType;
import com.example.storageservice.kafka.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = "test-topic", brokerProperties = {"listeners=PLAINTEXT://localhost:9092"}, ports = { 9092 })
class KafkaConsumerServiceTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ProfileDto profileDto;

    @BeforeEach
    public void init() {
        profileDto = new ProfileDto(
                1L,
                1234L,
                ProfileType.PROFILE_IMAGE,
                "asdf",
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString()
        );
    }

    @Test
    @DisplayName("kafka consumer 테스트")
    public void kafkaConsumerTest() throws InterruptedException {

        //given

        //when
        kafkaTemplate.send("test-topic", profileDto);
        boolean messageConsumed = kafkaConsumer.getLatch().await(10, TimeUnit.SECONDS);

        //then
        assertTrue(messageConsumed);
        assertEquals(kafkaConsumer.getReceivedMessage().getValue(), profileDto.getValue());
    }

    @Test
    @DisplayName("kafka producer 테스트")
    public void kafkaProducerTest() throws InterruptedException {

        //given

        //when
        kafkaProducer.sendMessage("test-topic", profileDto);
        boolean messageConsumed = kafkaConsumer.getLatch().await(10, TimeUnit.SECONDS);

        //then
        assertTrue(messageConsumed);
        assertEquals(kafkaConsumer.getReceivedMessage().getValue(), profileDto.getValue());
    }
}