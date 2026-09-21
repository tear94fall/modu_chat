package com.example.chatservice.chat.dto;

import com.example.chatservice.chat.entity.ChatReaction;
import com.example.chatservice.chat.entity.ReactionEmoji;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 메시지 하나의 이모지별 집계. userIds 로 "내가 남겼는지" 와 "누가 남겼는지" 를 앱이 안다. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryDto implements Serializable {
    private String emoji;
    private int count;
    private List<String> userIds;

    /** 한 메시지의 반응 줄들을 이모지별로 묶는다. 처음 남겨진 이모지가 앞에 온다. */
    public static List<ReactionSummaryDto> summarize(List<ChatReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) return Collections.emptyList();
        Map<ReactionEmoji, List<String>> byEmoji = new LinkedHashMap<>();
        for (ChatReaction r : reactions) {
            byEmoji.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>()).add(r.getUserId());
        }
        List<ReactionSummaryDto> result = new ArrayList<>();
        byEmoji.forEach((emoji, userIds) -> result.add(new ReactionSummaryDto(emoji.name(), userIds.size(), userIds)));
        return result;
    }
}
