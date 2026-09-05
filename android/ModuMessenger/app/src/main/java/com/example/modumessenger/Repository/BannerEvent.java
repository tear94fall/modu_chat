package com.example.modumessenger.Repository;

public class BannerEvent {

    private final String roomId;
    private final String sender;
    private final String message;
    private final int chatType;

    public BannerEvent(String roomId, String sender, String message, int chatType) {
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.chatType = chatType;
    }

    public String getRoomId() { return roomId; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public int getChatType() { return chatType; }
}
