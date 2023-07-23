package com.example.chatservice.chat.dto;

import com.example.chatservice.member.dto.MemberDto;
import com.example.chatservice.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto implements Serializable{
    private Long id;
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private String lastChatId;
    private String lastChatTime;
    private List<MemberDto> members = new ArrayList<>();

    public ChatRoomDto(ChatRoom chatRoom, List<MemberDto> members) {
        setRoomId(chatRoom.getRoomId());
        setRoomName(chatRoom.getRoomName());
        setRoomImage(chatRoom.getRoomImage());
        setLastChatMsg(chatRoom.getLastChatMsg());
        setLastChatId(chatRoom.getLastChatId());
        setLastChatTime(chatRoom.getLastChatTime());
        setMembers(members);
    }

    public void updateLastChat(String chatId, ChatDto chatDto) {
        setLastChatId(chatId);
        setLastChatMsg((chatDto.getChatType() == ChatType.TEXT.getChatType()) ?
                chatDto.getMessage() : ChatType.fromChatType(chatDto.getChatType()).getChatTypeStr());
        setLastChatTime(chatDto.getChatTime());
    }

    public boolean checkChatRoomMember(String userId) {
        List<MemberDto> chatRoomMembers = members.stream()
                .filter(memberDto -> memberDto.getUserId().equals(userId))
                .collect(Collectors.toList());

        return chatRoomMembers.size() == 0;
    }
}
