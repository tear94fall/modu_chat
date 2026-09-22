package com.example.chatservice.chat.repository

import java.util.Locale
import org.springframework.data.domain.Sort

/**
 * 백오피스 채팅방 목록에서 고를 수 있는 정렬. 요청의 `sort=roomName,asc` 같은 문자열을 허용 목록으로만 해석한다.
 * Pageable 의 sort 를 그대로 열면 아무 컬럼이나 지정할 수 있어 따로 둔다.
 * 목록에 값이 보이는 열은 모두 여기에 있다. 백오피스는 머리글을 눌러 정렬한다.
 */
enum class ChatRoomSort(private val order: Sort) {
    CREATED_DESC(Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id"))),
    CREATED_ASC(Sort.by(Sort.Order.asc("createdDate"), Sort.Order.asc("id"))),

    /** last_chat_time 은 "2024-01-01T00:00:00" 꼴 문자열이라 사전순 비교가 곧 시간순이다. */
    LAST_CHAT_DESC(Sort.by(Sort.Order.desc("lastChatTime"), Sort.Order.desc("id"))),
    LAST_CHAT_ASC(Sort.by(Sort.Order.asc("lastChatTime"), Sort.Order.asc("id"))),
    LAST_CHAT_MSG_ASC(Sort.by(Sort.Order.asc("lastChatMsg"), Sort.Order.asc("id"))),
    LAST_CHAT_MSG_DESC(Sort.by(Sort.Order.desc("lastChatMsg"), Sort.Order.desc("id"))),
    ROOM_NAME_ASC(Sort.by(Sort.Order.asc("roomName"), Sort.Order.asc("id"))),
    ROOM_NAME_DESC(Sort.by(Sort.Order.desc("roomName"), Sort.Order.desc("id"))),

    /**
     * 멤버 수는 엔티티 필드가 아니라 컬렉션 크기다. 속성 이름으로 된 [Sort] 로는 못 담아
     * [ChatRoomRepository.findAllOrderByMemberCountAsc] 쪽 JPQL 이 대신 정렬한다.
     * 여기서 [Sort.unsorted] 를 돌려줘야 Spring Data 가 그 JPQL 뒤에 ORDER BY 를 덧붙이지 않는다.
     */
    MEMBER_COUNT_ASC(Sort.unsorted()),
    MEMBER_COUNT_DESC(Sort.unsorted()),
    ;

    /** 마지막 키는 항상 id 라서 같은 값끼리도 순서가 고정된다. 멤버 수 정렬만 예외로 비어 있다. */
    fun sort(): Sort = order

    /** 이 정렬은 엔티티 속성으로 표현할 수 없어 전용 질의가 필요하다. */
    fun byMemberCount(): Boolean = this == MEMBER_COUNT_ASC || this == MEMBER_COUNT_DESC

    companion object {
        /** 지금까지의 기본 정렬. 백오피스 화면이 갑자기 달라지지 않게 그대로 둔다. */
        @JvmField
        val DEFAULT = CREATED_DESC

        /** "roomName", "roomName,asc", "LASTCHATTIME,DESC" 처럼 필드[,방향] 형태만 받는다. 방향이 없으면 asc. 모르는 값이면 null. */
        @JvmStatic
        fun parse(raw: String?): ChatRoomSort? {
            if (raw.isNullOrBlank()) {
                return null
            }
            val parts = raw.trim().lowercase(Locale.ROOT).split(",")
            if (parts.size > 2) {
                return null
            }
            val direction = if (parts.size == 2) parts[1] else "asc"
            if (direction != "asc" && direction != "desc") {
                return null
            }
            val asc = direction == "asc"
            return when (parts[0]) {
                "createddate" -> if (asc) CREATED_ASC else CREATED_DESC
                "lastchattime" -> if (asc) LAST_CHAT_ASC else LAST_CHAT_DESC
                "lastchatmsg" -> if (asc) LAST_CHAT_MSG_ASC else LAST_CHAT_MSG_DESC
                "roomname" -> if (asc) ROOM_NAME_ASC else ROOM_NAME_DESC
                "membercount" -> if (asc) MEMBER_COUNT_ASC else MEMBER_COUNT_DESC
                else -> null
            }
        }
    }
}
