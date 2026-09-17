package com.example.wsservice.fcm.dto;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.member.dto.MemberDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class FcmMessageDto {
    private String topic;
    private int type;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;

    public FcmMessageDto(ChatRoomDto chatRoomDto, ChatDto chatDto) {
        setTopic(chatRoomDto.getRoomId());
        setType(chatDto.getChatType());
        setTitle(chatRoomDto.getRoomName());
        setBody(chatDto.getMessage());
        setImage(null);

        List<MemberDto> members = chatRoomDto.getMembers() == null ? List.of() : chatRoomDto.getMembers();
        String senderName = members.stream()
                .filter(m -> chatDto.getSender() != null && chatDto.getSender().equals(m.getUserId()))
                .map(m -> m.getUsername() == null ? "" : m.getUsername())
                .findFirst()
                .orElse("");

        data = new HashMap<>();
        data.put("roomId", chatRoomDto.getRoomId());
        data.put("sender", chatDto.getSender());
        // 앱이 알림 제목/본문을 만들 때 쓴다. 수신자가 발신자에게 별칭을 붙여 두었으면 앱이 senderName 대신 별칭을 쓴다.
        data.put("senderName", senderName);
        data.put("memberCount", String.valueOf(members.size()));
    }

    public FcmMessageDto(String topic, int type, String title, String body, String image, String sender) {
        setTopic(topic);
        setType(type);
        setTitle(title);
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

