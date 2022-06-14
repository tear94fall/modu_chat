package com.example.modumessenger.entity;

import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    @SerializedName("roomId")
    private String roomId;
    @SerializedName("roomName")
    private String roomName;
    @SerializedName("roomImage")
    private String roomImage;
    @SerializedName("lastChatMsg")
    private String lastChatMsg;
    @SerializedName("lastChatTime")
    private String lastChatTime;
    @SerializedName("chatMember")
    private List<Member> members = new ArrayList<>();

    public String getRoomId() { return this.roomId; }
    public String getRoomName() { return this.roomName; }
    public String getRoomImage() { return this.roomImage; }
    public String getLastChatMsg() { return this.lastChatMsg; }
    public String getLastChatTime() { return this.lastChatTime; }
    public List<Member> getMembers() { return this.members; }

    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomImage(String roomImage) { this.roomImage = roomImage; }
    public void setLastChatMsg(String lastChatMsg) { this.lastChatMsg = lastChatMsg; }
    public void setLastChatTime(String lastChatTime) { this.lastChatTime = lastChatTime; }
    public void setMembers(List<Member> members) { this.members = members; }

    public ChatRoom(ChatRoomDto chatRoomDto) {
        setRoomId(chatRoomDto.getRoomId());
        setRoomName(chatRoomDto.getRoomName());
        setRoomImage(chatRoomDto.getRoomImage());
        setLastChatMsg(chatRoomDto.getLastChatMsg());
        setLastChatTime(chatRoomDto.getLastChatTime());
        chatRoomDto.getMembers().forEach(member -> {
            members.add(new Member(member));
        });
    }
}
