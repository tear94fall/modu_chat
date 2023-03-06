package com.example.wsservice.chat.client;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatRoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("chat-service")
public interface ChatFeignClient {

    @PostMapping("/chat")
    Long saveChat(@RequestBody ChatDto chatDto);

    @GetMapping("/chat/{chatId}")
    ChatDto getChat(@PathVariable("chatId") String chatId);

    @GetMapping("/chat/{roomId}/room")
    ChatRoomDto getChatRoom(@PathVariable("roomId") String roomId);

    @PostMapping("/chat/{roomId}/room")
    ChatRoomDto updateChatRoom(@PathVariable("roomId") String roomId, @RequestBody ChatRoomDto chatRoomDto);
}
