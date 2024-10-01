package com.example.chatstoreservice.message;

import com.example.chatstoreservice.chat.entity.Chat;
import com.example.chatstoreservice.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaMessageListener {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "modu.modu-chat.chat", containerFactory = "chatKafkaListener", groupId = "chat-store")
    public void debeziumListener(ConsumerRecord<String, ChatListener> consumerRecord, Acknowledgment acknowledgment) {
        try {
            ChatListener chatListener = consumerRecord.value();
            String op = chatListener.getPayload().getOp();
            ChatModel chatModel = chatListener.getPayload().getAfter();

            Chat chat = new Chat(chatModel);

            if (op.equals("c")) {
                chatService.createChat(chat);
            } else if (op.equals("u")) {
                chatService.updateChat(chat);
            } else if (op.equals("d")) {
                chatService.deleteChat(chat);
            } else {
                // do nothing
            }

            log.info(chatListener.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        acknowledgment.acknowledge();
    }
}
