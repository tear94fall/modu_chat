package com.example.chatstoreservice.chat.client;

import com.example.chatstoreservice.chat.dto.ChatDto;
import com.example.chatstoreservice.chat.dto.ChatRoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("chat-service")
public interface ChatFeignClient {

    @GetMapping("/chat/{chatId}")
    ChatDto getChat(@PathVariable("chatId") String chatId);

    @GetMapping("/chat/{roomId}/room")
    ChatRoomDto getChatRoom(@PathVariable("roomId") String roomId);
}
