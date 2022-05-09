package com.example.modumessenger.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDto {
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private LocalDateTime lastChatTime;
    private List<Long> userIds;

    public String getRoomId() { return this.roomId; }
    public String getRoomName() { return this.roomName; }
    public String getRoomImage() { return this.roomImage; }
    public String getLastChatMsg() { return this.lastChatMsg; }
    public LocalDateTime getLastChatTime() { return this.lastChatTime; }
    public List<Long> getUserIds() { return this.userIds; }
}
