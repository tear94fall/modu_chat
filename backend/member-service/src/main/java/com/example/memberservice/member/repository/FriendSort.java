package com.example.memberservice.member.repository;

import static com.example.memberservice.member.entity.QMemberFriend.memberFriend;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.NumberExpression;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 클라이언트가 고를 수 있는 친구 목록 정렬. 이름은 내가 정한 별칭(member_friend.friend_name) 기준이다. 요청의 {@code sort=name,asc} 같은 문자열을 허용 목록으로만 해석한다.
 * Spring 의 Pageable sort 를 그대로 열면 아무 컬럼이나 지정할 수 있고 "한글 먼저" 같은 복합 규칙도 못 담아서 따로 둔다.
 */
public enum FriendSort {
    NAME_ASC, NAME_DESC, EMAIL_ASC, EMAIL_DESC;

    /** '힣'(U+D7A3) 다음 문자. '가' <= 이름 < 이 값이면 한글 음절로 시작하는 이름이다. */
    private static final String AFTER_LAST_HANGUL_SYLLABLE = "힤";

    /** "name", "name,asc", "NAME,DESC" 처럼 필드[,방향] 형태만 받는다. 방향이 없으면 asc. */
    public static Optional<FriendSort> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.trim().toLowerCase(Locale.ROOT).split(",", -1);
        if (parts.length > 2) {
            return Optional.empty();
        }
        String direction = parts.length == 2 ? parts[1] : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            return Optional.empty();
        }
        return switch (parts[0]) {
            case "name" -> Optional.of(direction.equals("asc") ? NAME_ASC : NAME_DESC);
            case "email" -> Optional.of(direction.equals("asc") ? EMAIL_ASC : EMAIL_DESC);
            default -> Optional.empty();
        };
    }

    /** 이 정렬이 요구하는 ORDER BY. 마지막 키는 항상 id 라서 같은 값끼리도 순서가 고정된다. */
    public List<OrderSpecifier<?>> orders() {
        return switch (this) {
            case NAME_ASC -> List.of(nameGroup(0, 1).asc(), nameKey().asc(), memberFriend.friend.email.asc(), memberFriend.friend.id.asc());
            case NAME_DESC -> List.of(nameGroup(1, 0).asc(), nameKey().desc(), memberFriend.friend.email.desc(), memberFriend.friend.id.desc());
            case EMAIL_ASC -> List.of(memberFriend.friend.email.asc(), memberFriend.friend.id.asc());
            case EMAIL_DESC -> List.of(memberFriend.friend.email.desc(), memberFriend.friend.id.desc());
        };
    }

    /**
     * 이름(내가 정한 별칭, friend_name)을 세 그룹으로 나눈다. 빈 별칭은 방향과 상관없이 항상 마지막(2).
     * 한글 음절로 시작하는 이름과 그 외 이름의 순서는 인자로 받는다. 범위 비교라 DB 콜레이션과 무관하다.
     */
    private static NumberExpression<Integer> nameGroup(int hangulRank, int otherRank) {
        return new CaseBuilder()
                .when(memberFriend.friendName.eq("")).then(2)
                .when(memberFriend.friendName.goe("가").and(memberFriend.friendName.lt(AFTER_LAST_HANGUL_SYLLABLE))).then(hangulRank)
                .otherwise(otherRank);
    }

    /** 소문자로 비교해 대소문자 구분이 다른 H2/MySQL 에서 같은 순서가 나오게 한다. */
    private static ComparableExpressionBase<String> nameKey() {
        return memberFriend.friendName.lower();
    }
}
