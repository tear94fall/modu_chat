package com.example.chatservice.kafka.consumer

import com.example.chatservice.kafka.producer.KafkaProducerService
import com.example.chatservice.message.entity.ChatMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(private val kafkaProducerService: KafkaProducerService) {

    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    /**
     * topic-chat-save 를 topic-chat-broadcast 로 넘긴다. 받은 객체를 그대로 다시 보낸다 —
     * ws-service 가 실어 보낸 excludeUserIds(1:1 차단 제외 대상)까지 손대지 않고 전달해야
     * 다른 ws 인스턴스도 같은 판단을 할 수 있다.
     */
    @KafkaListener(
        topics = ["topic-chat-save"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun receive(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            val chatMessage = consumerRecord.value()

            kafkaProducerService.sendMessage(chatMessage.roomId!!, chatMessage)

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }
}
