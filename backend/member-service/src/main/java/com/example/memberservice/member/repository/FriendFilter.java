package com.example.memberservice.member.repository;

import static com.example.memberservice.member.entity.QMemberFriend.memberFriend;

import com.example.memberservice.member.entity.FriendStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.Locale;
import java.util.Optional;

/**
 * 친구 목록에서 어떤 상태의 친구를 볼지. 요청의 {@code filter=favorite} 같은 문자열을 허용 목록으로만 해석한다.
 * 기본값 {@link #NORMAL} 은 예전 동작(전체)과 달리 숨김·차단한 친구를 뺀다.
 */
public enum FriendFilter {
    /** 숨김·차단이 아닌 친구(즐겨찾기 포함). */
    NORMAL,
    /** 즐겨찾기한 친구. 차단하면 즐겨찾기가 꺼지므로 자연히 NORMAL 중에서만 나온다. */
    FAVORITE,
    HIDDEN,
    BLOCKED;

    /** "normal"/"favorite"/"hidden"/"blocked" 만 받는다. 값이 없으면 기본 NORMAL, 모르는 값이면 empty(호출자가 400). */
    public static Optional<FriendFilter> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(NORMAL);
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "normal" -> Optional.of(NORMAL);
            case "favorite" -> Optional.of(FAVORITE);
            case "hidden" -> Optional.of(HIDDEN);
            case "blocked" -> Optional.of(BLOCKED);
            default -> Optional.empty();
        };
    }

    /** 이 필터가 요구하는 where 절. */
    public BooleanExpression predicate() {
        return switch (this) {
            case NORMAL -> memberFriend.status.eq(FriendStatus.NORMAL);
            case FAVORITE -> memberFriend.status.eq(FriendStatus.NORMAL).and(memberFriend.favorite.isTrue());
            case HIDDEN -> memberFriend.status.eq(FriendStatus.HIDDEN);
            case BLOCKED -> memberFriend.status.eq(FriendStatus.BLOCKED);
        };
    }
}
