package com.example.modumessenger.message.service;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.client.MessageFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MessageFeignClient messageFeignClient;

    public void sendMessage(ChatDto chatDto) throws JsonProcessingException {
        rabbitTemplate.convertAndSend("modu-chat.exchange", "modu-chat.key", objectMapper.writeValueAsString(chatDto));
    }

    public ChatDto sendMsgToRabbitMq(ChatDto chatDto) {
        return messageFeignClient.sendMsg(chatDto);
    }
}
