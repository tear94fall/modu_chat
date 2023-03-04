package com.example.wsservice.chat.service;

import com.example.wsservice.chat.client.ChatFeignClient;
import com.example.wsservice.chat.dto.ChatRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatFeignClient chatFeignClient;

    public ChatRoomDto getChatRoom(String roomId) {
        return chatFeignClient.getChatRoom(roomId);
    }

    public ChatRoomDto updateChatRoom(String roomId, ChatRoomDto chatRoomDto) {
        return chatFeignClient.updateChatRoom(roomId, chatRoomDto);
    }
}
