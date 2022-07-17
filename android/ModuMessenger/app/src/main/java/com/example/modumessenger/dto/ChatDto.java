package com.example.modumessenger.dto;

public class ChatDto {
    private  Long id;
    private int chatType;
    private String roomId;
    private String sender;
    private String message;
    private String chatTime;

    public Long getId() { return this.id; }
    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
    public String getMessage() { return this.message; }
    public String getChatTime() { return this.chatTime; }
    public int getChatType() { return this.chatType; }

    public void setId(Long id) { this.id = id; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setSender(String sender) { this.sender = sender; }
    public void setMessage(String message) { this.message = message; }
    public void setChatTime(String chatTime) { this.chatTime = chatTime; }
    public void setChatType(int chatType) { this.chatType = chatType; }
}
