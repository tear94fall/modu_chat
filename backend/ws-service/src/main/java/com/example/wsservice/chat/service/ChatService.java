package com.example.wsservice.chat.service;

import com.example.wsservice.chat.client.ChatFeignClient;
import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ReactionRequestDto;
import com.example.wsservice.chat.dto.ReactionResultDto;
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

    /** 반응 토글. chat-service 가 거부하면(내 메시지·모르는 이모지) FeignException 이 난다. */
    public ReactionResultDto react(String roomId, String chatId, String userId, String emoji) {
        return chatFeignClient.react(roomId, chatId, userId, new ReactionRequestDto(emoji));
    }
}
