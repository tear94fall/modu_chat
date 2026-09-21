package com.example.chatservice.chat.entity;

import java.util.Arrays;
import java.util.Optional;

/** 반응 이모지 키. 유니코드 대신 키를 저장하고 앱이 그림으로 바꾼다(👍❤️😂😮😢🙏). */
public enum ReactionEmoji {
    LIKE, HEART, LAUGH, WOW, SAD, PRAY;

    public static Optional<ReactionEmoji> of(String raw) {
        if (raw == null) return Optional.empty();
        return Arrays.stream(values()).filter(e -> e.name().equalsIgnoreCase(raw.trim())).findFirst();
    }
}
