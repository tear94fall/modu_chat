package com.example.modumessenger.common.handler;

import com.example.modumessenger.chat.dto.ChatDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitListenHandler {

    private final ObjectMapper objectMapper;

//    @RabbitListener(queues = "modu-chat.queue")
//    public void receiveMessage(String message) throws JsonProcessingException {
//        ChatDto chatDto = objectMapper.readValue(message, ChatDto.class);
//        log.info("[consumer][message] {}", chatDto);
//    }
}
