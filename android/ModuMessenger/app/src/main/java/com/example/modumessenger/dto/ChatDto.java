package com.example.modumessenger.dto;

public class ChatDto {
    private int chatType;
    private String roomId;
    private String sender;
    private String message;

    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
    public String getMessage() { return this.message; }
    public int getChatType() { return this.chatType; }

    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setSender(String sender) { this.sender = sender; }
    public void setMessage(String message) { this.message = message; }
    public void setChatType(int chatType) { this.chatType = chatType; }
}
