package com.example.modumessenger.Adapter;

// Sample Chat
public class ChatBubble {
    private final String chatMsg;
    private final int viewType;

    public ChatBubble(String chatMsg, int viewType) {
        this.chatMsg = chatMsg;
        this.viewType = viewType;
    }

    public String getChatMsg() { return this.chatMsg; }
    public int getViewType() { return this.viewType; }
}

// View Type
class BubbleViewType {
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
}
