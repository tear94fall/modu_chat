package com.example.modumessenger.Adapter;

import com.example.modumessenger.dto.ChatDto;

// Sample Chat
public class ChatBubble {
    private String roomId;
    private String chatMsg;
    private String chatTime;
    private String sender;
    private int chatType;

    public ChatBubble(String roomId, String chatMsg, String chatTime, String sender, int viewType) {
        setRoomId(roomId);
        setChatMsg(chatMsg);
        setChatTime(chatTime);
        setSender(sender);
        setChatType(viewType);
    }

    public ChatBubble(ChatDto chatDto) {
        roomId = chatDto.getRoomId();
        chatMsg = chatDto.getMessage();
        chatType = chatDto.getChatType();
        chatTime = chatDto.getChatTime();
        sender = chatDto.getSender();
    }

    public String getRoomId() { return this.roomId; }
    public String getChatMsg() { return this.chatMsg; }
    public String getChatTime() { return this.chatTime; }
    public String getSender() { return this.sender; }
    public int getChatType() { return this.chatType; }

    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setChatMsg(String msg) { this.chatMsg = msg; }
    public void setChatTime(String time) { this.chatTime = time; }
    public void setSender(String sender) { this.sender = sender; }
    public void setChatType(int type) { this.chatType = type; }
}
