package com.example.modumessenger.Adapter;

import com.example.modumessenger.dto.ChatDto;

// Sample Chat
public class ChatBubble {
    private String chatMsg;
    private String chatTime;
    private String sender;
    private int viewType;

    public ChatBubble(String chatMsg, int viewType) {
        this.chatMsg = chatMsg;
        this.viewType = viewType;
    }

    public ChatBubble(ChatDto chatDto) {
        chatMsg = chatDto.getMessage();
        viewType = chatDto.getChatType();
        chatTime = chatDto.getChatTime();
        sender = chatDto.getSender();
    }

    public String getChatMsg() { return this.chatMsg; }
    public String getChatTime() { return this.chatTime; }
    public String getSender() { return this.sender; }
    public int getViewType() { return this.viewType; }

    public void setChatMsg(String msg) { this.chatMsg = msg; }
    public void getChatTime(String time) { this.chatTime = time; }
    public void getSender(String sender) { this.sender = sender; }
    public void setViewType(int type) { this.viewType = type; }
}
