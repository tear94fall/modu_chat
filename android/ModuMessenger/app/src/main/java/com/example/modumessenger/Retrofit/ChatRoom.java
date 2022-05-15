package com.example.modumessenger.Retrofit;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
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
    @SerializedName("userIds")
    private List<String> userIds;

    public String getRoomId() { return this.roomId; }
    public String getRoomName() { return this.roomName; }
    public String getRoomImage() { return this.roomImage; }
    public String getLastChatMsg() { return this.lastChatMsg; }
    public String getLastChatTime() { return this.lastChatTime; }
    public List<String> getUserIds() { return this.userIds; }

    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomImage(String roomImage) { this.roomImage = roomImage; }
    public void setLastChatMsg(String lastChatMsg) { this.lastChatMsg = lastChatMsg; }
    public void setLastChatTime(String lastChatTime) { this.lastChatTime = lastChatTime; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
}
