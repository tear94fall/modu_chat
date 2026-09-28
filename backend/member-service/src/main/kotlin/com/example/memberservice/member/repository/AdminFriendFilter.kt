package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.QMemberFriend.memberFriend
import com.querydsl.core.types.dsl.BooleanExpression
import java.util.Locale

/**
 * 백오피스 회원 상세의 친구 탭 필터. 앱용 [FriendFilter] 와 달리 ALL(숨김·차단 포함 전부)이 있고,
 * FAVORITE 는 숨긴 친구 중 즐겨찾기도 포함한다(차단하면 즐겨찾기가 꺼지므로 차단은 자연히 빠진다).
 */
enum class AdminFriendFilter {
    ALL, NORMAL, FAVORITE, HIDDEN, BLOCKED;

    /** 이 필터의 where 절. ALL 은 null(조건 없음). */
    fun predicate(): BooleanExpression? = when (this) {
        ALL -> null
        NORMAL -> memberFriend.status.eq(FriendStatus.NORMAL)
        FAVORITE -> memberFriend.favorite.isTrue.and(memberFriend.status.ne(FriendStatus.BLOCKED))
        HIDDEN -> memberFriend.status.eq(FriendStatus.HIDDEN)
        BLOCKED -> memberFriend.status.eq(FriendStatus.BLOCKED)
    }

    companion object {
        /** 대소문자 무시. 값이 없으면 ALL, 모르는 값이면 null(호출자가 400). */
        @JvmStatic
        fun parse(raw: String?): AdminFriendFilter? {
            if (raw.isNullOrBlank()) {
                return ALL
            }
            val name = raw.trim().uppercase(Locale.ROOT)
            return entries.firstOrNull { it.name == name }
        }
    }
}
