package com.example.chatservice.api.admin.dto;

import com.example.chatservice.chat.entity.ChatRoom;
import lombok.Getter;

@Getter
public class AdminChatRoomSummaryDto {
    private final Long id;
    private final String roomId;
    private final String roomName;
    private final String roomImage;
    private final int memberCount;
    private final String lastChatMsg;
    private final String lastChatTime;

    public AdminChatRoomSummaryDto(ChatRoom room) {
        this.id = room.getId();
        this.roomId = room.getRoomId();
        this.roomName = room.getRoomName();
        this.roomImage = room.getRoomImage();
        this.memberCount = room.getChatRoomMemberList() == null ? 0 : room.getChatRoomMemberList().size();
        this.lastChatMsg = room.getLastChatMsg();
        this.lastChatTime = room.getLastChatTime();
    }
}
