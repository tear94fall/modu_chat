package com.example.modumessenger.chat.dto;

import com.example.modumessenger.chat.entity.Chat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto implements Serializable {
    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;
}
