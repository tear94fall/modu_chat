package com.example.chatservice.api.kafka.consumer

import com.example.chatservice.application.message.ChatMessage
import com.example.chatservice.application.usecase.ChatRelayUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/** topic-chat-save 리스너. 받은 메시지를 유스케이스에 넘기고, 넘긴 뒤에만 오프셋을 커밋한다. */
@Component
class ChatSaveListener(private val chatRelayUseCase: ChatRelayUseCase) {

    private val log = LoggerFactory.getLogger(ChatSaveListener::class.java)

    @KafkaListener(
        topics = ["topic-chat-save"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun receive(consumerRecord: ConsumerRecord<String, ChatMessage>, acknowledgment: Acknowledgment) {
        try {
            chatRelayUseCase.relay(consumerRecord.value())

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }
}
