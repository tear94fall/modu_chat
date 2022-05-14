package com.example.modumessenger.dto;

public class ChatDto {
    public enum ChatType {
        ENTER, COMM
    }

    private ChatType chatType;
    private String roomId;
    private String sender;
    private String message;

    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
    public String getMessage() { return this.message; }
    public ChatType getChatType() { return this.chatType; }
}
