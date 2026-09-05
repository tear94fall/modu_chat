package com.example.modumessenger.dto;

import com.google.gson.annotations.SerializedName;

/** GET /chat/unread/{userId} 응답 한 건. 방 하나의 읽음 상태를 나타낸다. */
public class ChatRoomUnreadDto {

    @SerializedName("roomId")
    private String roomId;

    @SerializedName("lastSendChatId")
    private Long lastSendChatId;

    @SerializedName("lastReadChatId")
    private Long lastReadChatId;

    @SerializedName("unreadChatCount")
    private Long unreadChatCount;

    public String getRoomId() { return roomId; }
    public Long getLastSendChatId() { return lastSendChatId; }
    public Long getLastReadChatId() { return lastReadChatId; }
    public Long getUnreadChatCount() { return unreadChatCount; }
}
