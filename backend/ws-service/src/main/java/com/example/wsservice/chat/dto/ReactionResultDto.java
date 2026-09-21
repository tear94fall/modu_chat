package com.example.wsservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** chat-service 반응 토글 응답. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResultDto {
    private Long chatId;
    private String roomId;
    private String authorUserId;
    private boolean added;
    private String emoji;
    private List<ReactionSummaryDto> reactions;
}
