package com.example.wsservice.chat.dto;

import java.util.Map;

/** 이모지 키 → 글자. 푸시 본문에만 쓴다(앱은 자기 표를 가진다). */
public final class ReactionEmojiText {
    private static final Map<String, String> TEXT = Map.of(
            "LIKE", "👍", "HEART", "❤️", "LAUGH", "😂", "WOW", "😮", "SAD", "😢", "PRAY", "🙏");

    private ReactionEmojiText() {}

    public static String of(String key) {
        return key == null ? "" : TEXT.getOrDefault(key, "");
    }
}
