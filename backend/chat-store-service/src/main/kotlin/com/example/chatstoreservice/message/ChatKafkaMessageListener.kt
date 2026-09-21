package com.example.chatstoreservice.message

import com.example.chatstoreservice.chat.entity.Chat
import com.example.chatstoreservice.chat.service.ChatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/** Debezium 이 MySQL chat 테이블 변경을 실어 보내는 토픽을 받아 Mongo 미러에 반영한다. */
@Service
class ChatKafkaMessageListener(private val chatService: ChatService) {

    private val log = LoggerFactory.getLogger(ChatKafkaMessageListener::class.java)

    @KafkaListener(topics = ["modu.modu-chat.chat"], containerFactory = "chatKafkaListener", groupId = "chat-store")
    fun debeziumListener(consumerRecord: ConsumerRecord<String, ChatListener>, acknowledgment: Acknowledgment) {
        try {
            val chatListener = consumerRecord.value()
            val payload = chatListener.payload
            val chatModel = payload?.after
            if (payload != null && chatModel != null) {
                val chat = Chat(chatModel)
                when (payload.op) {
                    "c" -> chatService.createChat(chat)
                    "u" -> chatService.updateChat(chat)
                    "d" -> chatService.deleteChat(chat)
                    else -> Unit
                }
            }
            log.info(chatListener.toString())
        } catch (e: Exception) {
            log.error(e.message)
        }
        acknowledgment.acknowledge()
    }
}
