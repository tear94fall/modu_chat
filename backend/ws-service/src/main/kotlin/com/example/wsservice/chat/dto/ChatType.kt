package com.example.wsservice.chat.dto

enum class ChatType(val chatType: Int, val chatTypeStr: String) {
    TEXT(1, ""),
    IMAGE(2, "image"),
    FILE(3, "file"),
    AUDIO(4, "audio");

    companion object {
        private val byType = entries.associateBy { it.chatType }

        @JvmStatic
        fun fromChatType(type: Int): ChatType? = byType[type]
    }
}
