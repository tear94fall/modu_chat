package com.example.modumessenger.dto;

import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;

import java.util.List;

public class ChatRoomDto {
    private String roomId;
    private String roomName;
    private String roomImage;
    private String lastChatMsg;
    private String lastChatId;
    private String lastChatTime;
    private List<MemberDto> members;

    public String getRoomId() { return this.roomId; }
    public String getRoomName() { return this.roomName; }
    public String getRoomImage() { return this.roomImage; }
    public String getLastChatMsg() { return this.lastChatMsg; }
    public String getLastChatId() { return this.lastChatId; }
    public String getLastChatTime() { return this.lastChatTime; }
    public List<MemberDto> getMembers() { return this.members; }

    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomImage(String roomImage) { this.roomImage = roomImage; }
    public void setLastChatMsg(String lastChatMsg) { this.lastChatMsg = lastChatMsg; }
    public void setLastChatId(String lastCHatId) { this.lastChatId = lastCHatId; }
    public void setLastChatTime(String lastChatTime) { this.lastChatTime = lastChatTime; }
    public void setMembers(List<MemberDto> members) { this.members = members; }

    public ChatRoomDto(ChatRoom chatRoom) {
        setRoomId(chatRoom.getRoomId());
        setRoomName(chatRoom.getRoomName());
        setRoomImage(chatRoom.getRoomImage());
        setLastChatMsg(chatRoom.getLastChatMsg());
        setLastChatId(chatRoom.getLastChatId());
        setLastChatTime(chatRoom.getLastChatTime());
    }
}
