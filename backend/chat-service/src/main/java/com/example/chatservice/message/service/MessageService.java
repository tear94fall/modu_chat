package com.example.chatservice.message.service;

import com.example.chatservice.message.entity.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.queue.queue2.exchange}")
    private String wsExchangeName;

    @Value("${rabbitmq.queue.routing.key.queue2}")
    private String wsRoutingKey;

    public void produceMessage(String exchangeName, String routingKey, String message) {
        log.info("message sent: {}", message);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

    @Transactional
    @RabbitListener(queues = "${rabbitmq.queue.queue1.name}")
    public void consumeMessage(String message) throws JsonProcessingException {
        ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

        produceMessage(wsExchangeName, wsRoutingKey, objectMapper.writeValueAsString(chatMessage));

        log.info("Received message: {}", chatMessage.toString());
    }
}
