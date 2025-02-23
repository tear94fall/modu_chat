package com.example.chatstoreservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto implements Serializable {
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private String lastChatId;
    private String lastChatTime;
    private List<MemberDto> members = new ArrayList<>();

    public void updateLastChat(String chatId, ChatDto chatDto) {
        setLastChatId(chatId);
        setLastChatMsg((chatDto.getChatType() == ChatType.TEXT.getChatType()) ?
                chatDto.getMessage() : ChatType.fromChatType(chatDto.getChatType()).getChatTypeStr());
        setLastChatTime(chatDto.getChatTime());
    }

    public boolean checkChatRoomMember(String userId) {
        List<MemberDto> chatRoomMembers = members.stream()
                .filter(memberDto -> memberDto.getUserId().equals(userId))
                .toList();

        return chatRoomMembers.isEmpty();
    }
}
