package com.example.chatstoreservice.chat.dto

enum class ChatType(val chatType: Int, val chatTypeStr: String) {
    TEXT(1, ""),
    IMAGE(2, "image"),
    FILE(3, "file"),
    AUDIO(4, "audio"),

    /** chat-service 가 남기는 안내 행(초대·나감·이름 변경). 본문이 곧 문장이라 표식이 없다. */
    SYSTEM_INVITE(5, ""),
    SYSTEM_LEAVE(6, ""),
    SYSTEM_RENAME(7, "");

    companion object {
        private val byType = entries.associateBy { it.chatType }

        @JvmStatic
        fun fromChatType(type: Int): ChatType? = byType[type]
    }
}
