package com.example.modumessenger.fcm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RequestChatDto {
    private String chatRoomName;
    private String message;
    private String Image;
}
