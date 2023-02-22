package com.example.modumessenger.chat.dto;

import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.common.handler.WebSocketHandler;
import com.example.modumessenger.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto implements Serializable{
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private String lastChatId;
    private String lastChatTime;
    private List<MemberDto> members = new ArrayList<>();

    public ChatRoomDto(ChatRoom chatRoom) {
        setRoomId(chatRoom.getRoomId());
        setRoomName(chatRoom.getRoomName());
        setRoomImage(chatRoom.getRoomImage());
        setLastChatMsg(chatRoom.getLastChatMsg());
        setLastChatId(chatRoom.getLastChatId());
        setLastChatTime(chatRoom.getLastChatTime());

        chatRoom.getChatRoomMemberList().forEach(chatRoomMember -> {
            members.add(new MemberDto(chatRoomMember.getMember()));
        });
    }

    public void updateLastChat(String chatId, ChatDto chatDto) {
        setLastChatId(chatId);
        setLastChatMsg((chatDto.getChatType() == ChatType.TEXT.getChatType()) ?
                chatDto.getMessage() : ChatType.fromChatType(chatDto.getChatType()).getChatTypeStr());
        setLastChatTime(chatDto.getChatTime());
    }
}
