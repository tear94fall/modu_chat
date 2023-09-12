package com.example.modumessenger.dto;

public class FcmMessageDto {
    private String type;
    private String title;
    private String message;
    private String roomId;
    private String sender;

    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setSender(String sender) { this.sender = sender; }

    public String getType() { return this.type; }
    public String getTitle() { return this.title; }
    public String getMessage() { return this.message; }
    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
}
