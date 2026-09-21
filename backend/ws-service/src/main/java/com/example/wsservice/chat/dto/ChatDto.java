package com.example.wsservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto implements Serializable {
    private Long id;
    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;
    private ChatRoomDto chatRoomDto;
    /** 이모지별 반응 집계(chat-service 가 채운다). 새 메시지 브로드캐스트에는 비어 있다. */
    private List<ReactionSummaryDto> reactions;

    public ChatDto(String msg, String roomId, String sender, String chatTime, int type, ChatRoomDto chatRoomDto) {
        this.message = msg;
        this.roomId = roomId;
        this.sender = sender;
        this.chatTime = chatTime;
        this.chatType = type;
        this.chatRoomDto = chatRoomDto;
    }
}
