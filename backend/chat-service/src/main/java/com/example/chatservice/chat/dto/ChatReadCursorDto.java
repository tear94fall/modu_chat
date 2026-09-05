package com.example.chatservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 방 멤버 한 명의 읽음 커서. 말풍선 옆 안 읽음 수 계산의 원본이다. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadCursorDto {

    private String userId;
    private Long lastReadChatId;
}
