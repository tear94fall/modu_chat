package com.example.modumessenger.Global;

/** 화면에 보여줄 사람 이름. 내가 정한 별칭이 있으면 별칭, 아니면 상대가 정한 이름. 모든 이름 표시가 이 함수를 거친다. */
public final class DisplayName {

    private DisplayName() {}

    public static String of(String userId, String username) {
        return of(FriendNames.instance(), userId, username);
    }

    public static String of(FriendNames names, String userId, String username) {
        String alias = names.get(userId);
        if (alias != null && !alias.trim().isEmpty()) {
            return alias;
        }
        return username == null ? "" : username;
    }
}
