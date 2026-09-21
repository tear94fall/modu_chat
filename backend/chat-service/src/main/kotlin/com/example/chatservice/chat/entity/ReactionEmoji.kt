package com.example.chatservice.chat.entity

/** 반응 이모지 키. 유니코드 대신 키를 저장하고 앱이 그림으로 바꾼다(👍❤️😂😮😢🙏). */
enum class ReactionEmoji {
    LIKE, HEART, LAUGH, WOW, SAD, PRAY;

    companion object {
        /** 대소문자·공백을 무시하고 찾는다. 모르는 값이면 null. */
        @JvmStatic
        fun of(raw: String?): ReactionEmoji? {
            if (raw == null) return null
            val key = raw.trim()
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
        }
    }
}
