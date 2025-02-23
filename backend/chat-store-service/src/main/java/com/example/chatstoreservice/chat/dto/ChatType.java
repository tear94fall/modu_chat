package com.example.chatstoreservice.chat.dto;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ChatType {
    TEXT(1, ""),
    IMAGE(2, "image"),
    FILE(3, "file"),
    AUDIO(4, "audio");

    private final int chatType;
    private final String chatTypeStr;

    private static final Map<Integer, ChatType> chatTypeMap = new HashMap<>();

    static {
        for(ChatType chatType : values()) {
            chatTypeMap.put(chatType.getChatType(), chatType);
        }
    }

    ChatType(int chatType, String chatTypeStr) {
        this.chatType = chatType;
        this.chatTypeStr = chatTypeStr;
    }

    public static ChatType fromChatType(int type) {
        return chatTypeMap.get(type);
    }
}
