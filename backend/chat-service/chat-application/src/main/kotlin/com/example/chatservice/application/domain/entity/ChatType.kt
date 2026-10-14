package com.example.chatservice.application.domain.entity

object ChatType {
    const val CHAT_TYPE_TEXT = 1
    const val CHAT_TYPE_IMAGE = 2
    const val CHAT_TYPE_FILE = 3
    const val CHAT_TYPE_AUDIO = 4

    /** 시스템 안내 — 서버가 만든다. 본문은 사람이 읽을 문장이고 sender 는 그 사건의 주인공 userId 다(초대: 초대한 사람, 나감: 나간 사람). */
    const val CHAT_TYPE_SYSTEM_INVITE = 5
    const val CHAT_TYPE_SYSTEM_LEAVE = 6
    const val CHAT_TYPE_SYSTEM_RENAME = 7

    /** 사람이 보낸 글이 아닌 안내 행. 안 읽음 개수·푸시에서 뺀다. */
    val SYSTEM_TYPES: Set<Int> = setOf(CHAT_TYPE_SYSTEM_INVITE, CHAT_TYPE_SYSTEM_LEAVE, CHAT_TYPE_SYSTEM_RENAME)
    fun isSystem(type: Int): Boolean = type in SYSTEM_TYPES
}
