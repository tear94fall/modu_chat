package com.example.modumessenger.Retrofit;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoom {
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private LocalDateTime lastChatTime;
    private List<Long> userIds;
}
