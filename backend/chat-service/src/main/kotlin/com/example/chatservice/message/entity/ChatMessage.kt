package com.example.chatservice.message.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * topic-chat-save 로 들어와 topic-chat-broadcast 로 나가는 메시지.
 *
 * excludeUserIds 는 ws-service 가 1:1 차단 때 넣는 "이 세션들에는 보내지 말 것" 목록이다.
 * chat-service 는 판단하지 않고 그대로 실어 보낸다 — 값을 잃으면 다른 인스턴스의
 * 브로드캐스트가 차단을 무시하게 된다.
 *
 * 필드가 없던 시절의 메시지가 아직 토픽에 남아 있을 수 있어 ignoreUnknown 을 켜고,
 * 값이 없을 때는 JSON 에 키 자체를 넣지 않는다(기존 소비자와 동일한 모양).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatMessage(
    var type: SubscribeType? = null,
    var roomId: String? = null,
    var chatId: String? = null,
    /** 전달 제외 대상 userId. null/빈 목록이면 제외 없음. */
    var excludeUserIds: List<String>? = null,
)
