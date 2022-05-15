package com.example.modumessenger.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
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
    private String lastChatTime;
    private List<String> userIds;
}
