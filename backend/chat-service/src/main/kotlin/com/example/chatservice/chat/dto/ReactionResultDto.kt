package com.example.chatservice.chat.dto

/** 반응 토글 결과. ws-service 가 브로드캐스트와 푸시를 만드는 재료다. */
data class ReactionResultDto(
    var chatId: Long? = null,
    var roomId: String? = null,
    /** 메시지 작성자. 푸시 대상. */
    var authorUserId: String? = null,
    /** true 면 반응이 남겨졌다(생성·교체), false 면 취소됐다. 취소는 푸시하지 않는다. */
    var added: Boolean = false,
    /** 남겨진 이모지 키. 취소면 null. */
    var emoji: String? = null,
    var reactions: List<ReactionSummaryDto>? = null,
)
