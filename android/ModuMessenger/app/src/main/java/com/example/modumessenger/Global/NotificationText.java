package com.example.modumessenger.Global;

/**
 * 채팅 푸시의 제목/본문. 서버는 발신자 userId 와 그 사람의 현재 이름(senderName)만 주고,
 * "내가 정한 이름" 은 이 기기에 저장된 별칭 맵에서 찾는다.
 * 1:1 방(참여자 2명)은 제목 = 발신자, 그 외에는 제목 = 방 이름, 본문 = "발신자: 메시지".
 */
public final class NotificationText {

    public static final String IMAGE_BODY = "새로운 사진";

    private final String title;
    private final String body;

    private NotificationText(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() { return title; }
    public String getBody() { return body; }

    public static NotificationText build(FriendNames names, String roomName, String sender, String senderName,
                                         String memberCount, String message, boolean isImage) {
        String who = DisplayName.of(names, sender, senderName);
        String text = isImage ? IMAGE_BODY : (message == null ? "" : message);
        String room = roomName == null ? "" : roomName;

        if (who.isEmpty()) {
            return new NotificationText(room, text);
        }
        if (isOneOnOne(memberCount)) {
            return new NotificationText(who, text);
        }
        return new NotificationText(room, who + ": " + text);
    }

    private static boolean isOneOnOne(String memberCount) {
        try {
            return memberCount != null && Integer.parseInt(memberCount.trim()) == 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
