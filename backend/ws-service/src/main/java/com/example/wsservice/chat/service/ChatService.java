package com.example.wsservice.chat.service;

import com.example.wsservice.chat.client.ChatFeignClient;
import com.example.wsservice.chat.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatFeignClient chatFeignClient;

    public Long saveChat(ChatDto chatDto) {
        return chatFeignClient.saveChat(chatDto);
    }

    public ChatDto getChat(String chatId) {
        return chatFeignClient.getChat(chatId);
    }
}
