package com.example.wsservice.chat.dto

/** 이모지 키 → 글자. 푸시 본문에만 쓴다(앱은 자기 표를 가진다). */
object ReactionEmojiText {
    private val TEXT = mapOf("LIKE" to "👍", "HEART" to "❤️", "LAUGH" to "😂", "WOW" to "😮", "SAD" to "😢", "PRAY" to "🙏")

    @JvmStatic
    fun of(key: String?): String = if (key == null) "" else TEXT[key] ?: ""
}
