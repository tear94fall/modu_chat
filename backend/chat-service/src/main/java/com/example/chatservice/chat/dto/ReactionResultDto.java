package com.example.chatservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 반응 토글 결과. ws-service 가 브로드캐스트와 푸시를 만드는 재료다. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResultDto {
    private Long chatId;
    private String roomId;
    /** 메시지 작성자. 푸시 대상. */
    private String authorUserId;
    /** true 면 반응이 남겨졌다(생성·교체), false 면 취소됐다. 취소는 푸시하지 않는다. */
    private boolean added;
    /** 남겨진 이모지 키. 취소면 null. */
    private String emoji;
    private List<ReactionSummaryDto> reactions;
}
