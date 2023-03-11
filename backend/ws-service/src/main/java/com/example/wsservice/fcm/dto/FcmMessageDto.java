package com.example.wsservice.fcm.dto;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatRoomDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class FcmMessageDto {
    private String topic;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;

    public FcmMessageDto(ChatRoomDto chatRoomDto, ChatDto chatDto) {
        setTopic(chatRoomDto.getRoomId());
        setTitle(chatRoomDto.getRoomName());
        setBody(chatDto.getMessage());
        setImage(null);

        data = new HashMap<>() {
            {
                put("roomId", chatRoomDto.getRoomId());
                put("sender", chatDto.getSender());
            }
        };
    }

    public FcmMessageDto(String topic, String title, String body, String image, String sender) {
        setTopic(topic);
        setTopic(title);
        setBody(body);
        setImage(image);

        data = new HashMap<>() {
            {
                put("roomId", topic);
                put("sender", sender);
            }
        };
    }
}

