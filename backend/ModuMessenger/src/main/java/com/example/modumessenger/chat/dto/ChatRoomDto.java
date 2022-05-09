package com.example.modumessenger.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDto {
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private LocalDateTime lastChatTime;
    private List<Long> userIds;
}
