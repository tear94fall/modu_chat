package com.example.chatstoreservice.message

/** Debezium 이벤트 봉투. 전용 ObjectMapper(Kotlin 모듈 없음)가 기본 생성자 + setter 로 채운다. */
class ChatListener {
    var payload: ChatPayload? = null

    override fun toString(): String = "ChatListener(payload=$payload)"
}
