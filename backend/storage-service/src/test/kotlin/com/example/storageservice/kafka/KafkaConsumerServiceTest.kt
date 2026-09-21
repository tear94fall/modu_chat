package com.example.storageservice.kafka

import com.example.storageservice.kafka.consumer.KafkaConsumer
import com.example.storageservice.kafka.dto.ProfileDto
import com.example.storageservice.kafka.dto.ProfileType
import com.example.storageservice.kafka.producer.KafkaProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = ["test-topic"], brokerProperties = ["listeners=PLAINTEXT://localhost:9092"], ports = [9092])
class KafkaConsumerServiceTest {

    @Autowired lateinit var kafkaProducer: KafkaProducer

    @Autowired lateinit var kafkaConsumer: KafkaConsumer

    @Autowired lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var profileDto: ProfileDto

    @BeforeEach
    fun init() {
        profileDto = ProfileDto(1L, 1234L, ProfileType.PROFILE_IMAGE, "asdf", LocalDateTime.now().toString(), LocalDateTime.now().toString())
    }

    @Test
    @DisplayName("kafka consumer 테스트")
    fun kafkaConsumerTest() {
        kafkaTemplate.send("test-topic", profileDto)
        val messageConsumed = kafkaConsumer.latch.await(10, TimeUnit.SECONDS)

        assertTrue(messageConsumed)
        assertEquals(profileDto.value, kafkaConsumer.receivedMessage?.value)
    }

    @Test
    @DisplayName("kafka producer 테스트")
    fun kafkaProducerTest() {
        kafkaProducer.sendMessage("test-topic", profileDto)
        val messageConsumed = kafkaConsumer.latch.await(10, TimeUnit.SECONDS)

        assertTrue(messageConsumed)
        assertEquals(profileDto.value, kafkaConsumer.receivedMessage?.value)
    }
}
