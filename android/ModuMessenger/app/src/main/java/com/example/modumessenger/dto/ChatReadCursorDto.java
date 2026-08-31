package com.example.modumessenger.dto;

import com.google.gson.annotations.SerializedName;

/** GET /chat/read/{roomId} 응답 한 건. 방 멤버 한 명의 읽음 커서다. */
public class ChatReadCursorDto {

    @SerializedName("userId")
    private String userId;

    @SerializedName("lastReadChatId")
    private Long lastReadChatId;

    public String getUserId() { return userId; }
    public Long getLastReadChatId() { return lastReadChatId; }
}
