package com.example.storageservice.kafka.consumer

import com.example.storageservice.kafka.dto.ProfileDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch

/** 테스트 전용 리스너. 임베디드 Kafka 의 test-topic 을 받는다. */
@Service
class KafkaConsumer {

    val latch = CountDownLatch(1)
    var receivedMessage: ProfileDto? = null

    @KafkaListener(topics = ["test-topic"], groupId = "test-group")
    fun listen(record: ConsumerRecord<String, Any>) {
        receivedMessage = ObjectMapper().convertValue(record.value(), ProfileDto::class.java)
        latch.countDown()
    }
}
