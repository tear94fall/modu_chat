package com.example.wsservice.chat.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * ws 인스턴스들 사이를 Kafka 로 오가는 봉투. 필드가 없는 옛 메시지(그리고 필드를 모르는 옛 컨슈머)와 양방향으로 호환되게 한다.
 * 선언 순서가 곧 JSON 순서다(옛 자바와 같은 모양을 테스트가 고정한다).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatMessage(
    var type: SubscribeType? = null,
    var roomId: String? = null,
    var chatId: String? = null,
    /** type=READ 일 때만 채워진다. 읽은 사람의 userId. type=REACTION 이면 반응자. */
    var userId: String? = null,
    /**
     * 이 메시지를 전달하지 않을 userId 들(1:1 방에서 발신자를 차단한 상대). null/빈 = 제외 없음.
     * 다른 ws 인스턴스도 같은 판단을 하도록 값을 메시지에 실어 보낸다.
     */
    var excludeUserIds: List<String>? = null,
    /** type=REACTION: 메시지 작성자(푸시 대상), 남겨진 이모지 키(취소면 null), 남겨졌는지, 갱신된 집계. */
    var authorUserId: String? = null,
    var emoji: String? = null,
    var added: Boolean? = null,
    var reactions: List<ReactionSummaryDto>? = null,
) {
    companion object {
        /** 반응 브로드캐스트. */
        @JvmStatic
        fun reaction(result: ReactionResultDto, reactorUserId: String): ChatMessage = ChatMessage(
            type = SubscribeType.REACTION,
            roomId = result.roomId,
            chatId = result.chatId.toString(),
            userId = reactorUserId,
            authorUserId = result.authorUserId,
            emoji = result.emoji,
            added = result.added,
            reactions = result.reactions,
        )
    }
}
