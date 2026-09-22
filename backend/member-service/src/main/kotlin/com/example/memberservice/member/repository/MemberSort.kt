package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.QMember.member
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.core.types.dsl.NumberExpression
import java.util.Locale

/**
 * 백오피스 회원 목록에서 고를 수 있는 정렬. [FriendSort] 와 같은 이유로 따로 둔다 —
 * Pageable 의 sort 를 그대로 열면 아무 컬럼이나 지정할 수 있고 "한글 먼저" 같은 복합 규칙도 담을 수 없다.
 * 이름 정렬 규칙은 앱 친구 목록([FriendSort])과 같아야 한다. 같은 회원이 화면마다 다른 자리에 있으면 헷갈린다.
 * 목록에 값이 보이는 열은 모두 여기에 있다. 백오피스는 머리글을 눌러 정렬한다.
 */
enum class MemberSort {
    NAME_ASC, NAME_DESC,
    EMAIL_ASC, EMAIL_DESC,
    USER_ID_ASC, USER_ID_DESC,
    ROLE_ASC, ROLE_DESC,
    CREATED_DESC, CREATED_ASC,
    ;

    /** 이 정렬이 요구하는 ORDER BY. 마지막 키는 항상 id 라서 같은 값끼리도 순서가 고정된다. */
    fun orders(): List<OrderSpecifier<*>> = when (this) {
        NAME_ASC -> listOf(nameGroup(0, 1).asc(), nameKey().asc(), member.id.asc())
        NAME_DESC -> listOf(nameGroup(1, 0).asc(), nameKey().desc(), member.id.desc())
        EMAIL_ASC -> listOf(member.email.lower().asc(), member.id.asc())
        EMAIL_DESC -> listOf(member.email.lower().desc(), member.id.desc())
        USER_ID_ASC -> listOf(member.userId.lower().asc(), member.id.asc())
        USER_ID_DESC -> listOf(member.userId.lower().desc(), member.id.desc())
        // role 은 EnumType.STRING 이라 저장된 이름("ROLE_ADMIN" < "ROLE_MEMBER") 순으로 줄선다.
        ROLE_ASC -> listOf(member.role.asc(), member.id.asc())
        ROLE_DESC -> listOf(member.role.desc(), member.id.desc())
        CREATED_DESC -> listOf(member.createdDate.desc(), member.id.desc())
        CREATED_ASC -> listOf(member.createdDate.asc(), member.id.asc())
    }

    companion object {
        /** 백오피스 기본값. 이름 가나다순이 목록에서 사람을 찾기 쉽다. */
        @JvmField
        val DEFAULT = NAME_ASC

        /** '힣'(U+D7A3) 다음 문자. '가' <= 이름 < 이 값이면 한글 음절로 시작하는 이름이다. */
        private const val AFTER_LAST_HANGUL_SYLLABLE = "힤"

        /** "name", "name,asc", "createdDate,desc" 처럼 필드[,방향] 형태만 받는다. 방향이 없으면 asc. */
        @JvmStatic
        fun parse(raw: String?): MemberSort? {
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
                "name" -> if (asc) NAME_ASC else NAME_DESC
                "email" -> if (asc) EMAIL_ASC else EMAIL_DESC
                "userid" -> if (asc) USER_ID_ASC else USER_ID_DESC
                "role" -> if (asc) ROLE_ASC else ROLE_DESC
                "createddate" -> if (asc) CREATED_ASC else CREATED_DESC
                else -> null
            }
        }

        /**
         * 이름을 세 그룹으로 나눈다. 이름이 비었거나 없는 회원은 방향과 상관없이 항상 마지막(2) —
         * 빈칸이 목록 맨 위에 몰려 있으면 회원을 찾는 데 방해만 된다.
         * 한글 음절로 시작하는 이름과 그 외(영문·숫자·기호) 이름의 순서는 인자로 받는다.
         * 범위 비교라 DB 콜레이션과 무관하다.
         */
        private fun nameGroup(hangulRank: Int, otherRank: Int): NumberExpression<Int> =
            CaseBuilder()
                .`when`(member.username.isNull.or(member.username.eq(""))).then(2)
                .`when`(member.username.goe("가").and(member.username.lt(AFTER_LAST_HANGUL_SYLLABLE))).then(hangulRank)
                .otherwise(otherRank)

        /** 소문자로 비교해 대소문자 구분이 다른 H2/MySQL 에서 같은 순서가 나오게 한다. 이메일·아이디도 같은 이유로 lower 다. */
        private fun nameKey(): ComparableExpressionBase<String> = member.username.lower()
    }
}
