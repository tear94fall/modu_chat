package com.example.chatstoreservice.message;

import com.example.chatstoreservice.chat.client.ChatFeignClient;
import com.example.chatstoreservice.chat.dto.ChatDto;
import com.example.chatstoreservice.chat.dto.ChatRoomDto;
import com.example.chatstoreservice.chat.entity.Chat;
import com.example.chatstoreservice.chat.repository.ChatRepository;
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
    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;
    private final ChatFeignClient chatFeignClient;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Transactional
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeMessage(String message) throws JsonProcessingException {
        ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

        ChatDto chat = chatFeignClient.getChat(chatMessage.getChatId());
        ChatRoomDto chatRoom = chatFeignClient.getChatRoom(chatMessage.getRoomId());

        chatRepository.save(new Chat(chat));

        log.info("Received message: {}", chatMessage.toString());
    }
}
