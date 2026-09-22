package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.QMemberFriend.memberFriend
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.core.types.dsl.NumberExpression
import java.util.Locale

/**
 * 클라이언트가 고를 수 있는 친구 목록 정렬. 이름은 내가 정한 별칭(member_friend.friend_name) 기준이다. 요청의 `sort=name,asc` 같은 문자열을 허용 목록으로만 해석한다.
 * Spring 의 Pageable sort 를 그대로 열면 아무 컬럼이나 지정할 수 있고 "한글 먼저" 같은 복합 규칙도 못 담아서 따로 둔다.
 */
enum class FriendSort {
    NAME_ASC, NAME_DESC, EMAIL_ASC, EMAIL_DESC;

    /** 이 정렬이 요구하는 ORDER BY. 마지막 키는 항상 id 라서 같은 값끼리도 순서가 고정된다. */
    fun orders(): List<OrderSpecifier<*>> = when (this) {
        NAME_ASC -> listOf(nameGroup(0, 1).asc(), nameKey().asc(), memberFriend.friend.email.asc(), memberFriend.friend.id.asc())
        NAME_DESC -> listOf(nameGroup(1, 0).asc(), nameKey().desc(), memberFriend.friend.email.desc(), memberFriend.friend.id.desc())
        EMAIL_ASC -> listOf(memberFriend.friend.email.asc(), memberFriend.friend.id.asc())
        EMAIL_DESC -> listOf(memberFriend.friend.email.desc(), memberFriend.friend.id.desc())
    }

    companion object {
        /** '힣'(U+D7A3) 다음 문자. '가' <= 이름 < 이 값이면 한글 음절로 시작하는 이름이다. */
        private const val AFTER_LAST_HANGUL_SYLLABLE = "힤"

        /** "name", "name,asc", "NAME,DESC" 처럼 필드[,방향] 형태만 받는다. 방향이 없으면 asc. */
        @JvmStatic
        fun parse(raw: String?): FriendSort? {
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
            return when (parts[0]) {
                "name" -> if (direction == "asc") NAME_ASC else NAME_DESC
                "email" -> if (direction == "asc") EMAIL_ASC else EMAIL_DESC
                else -> null
            }
        }

        /**
         * 이름(내가 정한 별칭, friend_name)을 세 그룹으로 나눈다. 빈 별칭은 방향과 상관없이 항상 마지막(2).
         * 한글 음절로 시작하는 이름과 그 외 이름의 순서는 인자로 받는다. 범위 비교라 DB 콜레이션과 무관하다.
         */
        private fun nameGroup(hangulRank: Int, otherRank: Int): NumberExpression<Int> =
            CaseBuilder()
                .`when`(memberFriend.friendName.eq("")).then(2)
                .`when`(memberFriend.friendName.goe("가").and(memberFriend.friendName.lt(AFTER_LAST_HANGUL_SYLLABLE))).then(hangulRank)
                .otherwise(otherRank)

        /** 소문자로 비교해 대소문자 구분이 다른 H2/MySQL 에서 같은 순서가 나오게 한다. */
        private fun nameKey(): ComparableExpressionBase<String> = memberFriend.friendName.lower()
    }
}
