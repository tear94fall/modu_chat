package com.example.chatservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomLastReadChatDto {

    private String roomId;
    private Long lastSendChatId;
    private Long lastReadChatId;
    private Long unreadChatCount;

    public void updateUnreadChatCount(Long unreadChatCount) {
        this.unreadChatCount = unreadChatCount;
    }

    public static ChatRoomLastReadChatDto createChatRoomLastReadChatDto(String roomId, Long lastSendChatId, Long lastReadChatId, Long unreadChatCount) {
        return ChatRoomLastReadChatDto.builder()
                .roomId(roomId)
                .lastSendChatId(lastSendChatId)
                .lastReadChatId(lastReadChatId)
                .unreadChatCount(unreadChatCount)
                .build();
    }
}
