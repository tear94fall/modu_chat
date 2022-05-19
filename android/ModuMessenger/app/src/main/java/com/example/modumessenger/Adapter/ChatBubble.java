package com.example.modumessenger.Adapter;

import com.example.modumessenger.dto.ChatBubbleViewType;
import com.example.modumessenger.dto.ChatDto;

// Sample Chat
public class ChatBubble {
    private String chatMsg;
    private int viewType;

    public ChatBubble(String chatMsg, int viewType) {
        this.chatMsg = chatMsg;
        this.viewType = viewType;
    }

    public ChatBubble(ChatDto chatDto) {
        chatMsg = chatDto.getMessage();
        viewType = chatDto.getChatType();
    }

    public String getChatMsg() { return this.chatMsg; }
    public int getViewType() { return this.viewType; }

    public void setChatMsg(String msg) { this.chatMsg = msg; }
    public void setViewType(int type) { this.viewType = ChatBubbleViewType.RIGHT; }
}
