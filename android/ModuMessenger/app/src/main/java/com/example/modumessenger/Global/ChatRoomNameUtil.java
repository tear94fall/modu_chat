package com.example.modumessenger.Global;

import com.example.modumessenger.entity.Member;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅방 목록과 채팅방 화면이 서로 다른 규칙으로 방 이름을 정하고 있었다.
 * 목록은 사람이 둘 이상이면 참여자 이름으로 덮어써서, 사용자가 지은 방 이름이
 * 나올 자리가 없었다. 규칙을 한 곳에 두어 두 화면이 갈라지지 않게 한다.
 */
public class ChatRoomNameUtil {

    /** 방을 만들 때 서버가 넣는 기본 이름. 이 값은 "직접 지은 이름" 으로 보지 않는다. */
    public static final String DEFAULT_ROOM_NAME = "새로운 채팅방";

    public static boolean hasCustomName(String roomName) {
        return roomName != null && !roomName.trim().isEmpty() && !roomName.equals(DEFAULT_ROOM_NAME);
    }

    /** 직접 넣은 방 사진이 있는지. 있으면 참여자 사진 대신 이것을 쓴다. */
    public static boolean hasCustomImage(String roomImage) {
        return roomImage != null && !roomImage.trim().isEmpty();
    }

    /**
     * 화면에 보일 방 이름. 직접 지은 이름이 있으면 그것을 쓰고, 없을 때만 참여자 이름으로 만든다.
     *
     * @param maxLength 0 이하면 자르지 않는다. 목록처럼 한 줄에 담아야 하는 곳에서만 쓴다.
     */
    public static String resolve(String roomName, List<Member> members,
                                 String myUserId, String myUsername, int maxLength) {
        if (hasCustomName(roomName)) {
            return truncate(roomName, maxLength);
        }

        List<String> others = members == null ? List.of() : members.stream()
                .filter(m -> m.getUserId() != null && !m.getUserId().equals(myUserId))
                .map(Member::getUsername)
                .collect(Collectors.toList());

        if (others.isEmpty()) {
            return String.format("나와의 채팅 (%s)", myUsername);
        }

        return truncate(String.join(", ", others), maxLength);
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }

        String cut = value.substring(0, maxLength).trim();
        return cut.endsWith(",") ? cut.substring(0, cut.length() - 1) : cut;
    }
}
