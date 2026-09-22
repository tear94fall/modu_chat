package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.QMemberFriend.memberFriend
import com.querydsl.core.types.dsl.BooleanExpression
import java.util.Locale

/**
 * 친구 목록에서 어떤 상태의 친구를 볼지. 요청의 `filter=favorite` 같은 문자열을 허용 목록으로만 해석한다.
 * 기본값 [NORMAL] 은 예전 동작(전체)과 달리 숨김·차단한 친구를 뺀다.
 */
enum class FriendFilter {
    /** 숨김·차단이 아닌 친구(즐겨찾기 포함). */
    NORMAL,

    /** 즐겨찾기한 친구. 차단하면 즐겨찾기가 꺼지므로 자연히 NORMAL 중에서만 나온다. */
    FAVORITE,
    HIDDEN,
    BLOCKED,
    ;

    /** 이 필터가 요구하는 where 절. */
    fun predicate(): BooleanExpression = when (this) {
        NORMAL -> memberFriend.status.eq(FriendStatus.NORMAL)
        FAVORITE -> memberFriend.status.eq(FriendStatus.NORMAL).and(memberFriend.favorite.isTrue)
        HIDDEN -> memberFriend.status.eq(FriendStatus.HIDDEN)
        BLOCKED -> memberFriend.status.eq(FriendStatus.BLOCKED)
    }

    companion object {
        /** "normal"/"favorite"/"hidden"/"blocked" 만 받는다. 값이 없으면 기본 NORMAL, 모르는 값이면 null(호출자가 400). */
        @JvmStatic
        fun parse(raw: String?): FriendFilter? {
            if (raw.isNullOrBlank()) {
                return NORMAL
            }
            return when (raw.trim().lowercase(Locale.ROOT)) {
                "normal" -> NORMAL
                "favorite" -> FAVORITE
                "hidden" -> HIDDEN
                "blocked" -> BLOCKED
                else -> null
            }
        }
    }
}
