package com.example.modumessenger.core.model

/**
 * 채팅 한 줄. [id] 가 음수면 아직 서버 에코가 돌아오지 않은 낙관적 임시 메시지다.
 * [unreadCount] 는 읽음 커서로 계산한 "아직 안 읽은 사람 수"다.
 */
data class ChatMessage(
    val id: Long = 0L,
    val chatType: Int = ChatType.TEXT,
    val roomId: String = "",
    val sender: String = "",
    val message: String = "",
    val chatTime: String = "",
    val unreadCount: Int = 0,
    val status: SendStatus = SendStatus.SENT,
    /** 이모지별 반응 집계. 이력 조회와 REACTION 프레임으로 채워진다. */
    val reactions: List<Reaction> = emptyList(),
)

enum class SendStatus { SENDING, SENT, FAILED }

/** 서버와 주고받는 chatType 정수. */
object ChatType {
    const val INVALID = 0
    const val TEXT = 1
    const val IMAGE = 2
    const val FILE = 3
    const val AUDIO = 4

    /**
     * 서버가 남기는 안내 행. 본문은 사람이 읽을 문장이고 sender 는 그 사건의 주인공이다
     * (초대: 초대한 사람, 나감: 나간 사람, 이름 변경: 바꾼 사람).
     */
    const val SYSTEM_INVITE = 5
    const val SYSTEM_LEAVE = 6
    const val SYSTEM_RENAME = 7

    fun isSystem(chatType: Int): Boolean = chatType == SYSTEM_INVITE || chatType == SYSTEM_LEAVE || chatType == SYSTEM_RENAME
}
