package com.example.modumessenger.Global.socket;

import com.example.modumessenger.dto.ChatDto;

public interface ChatSocketListener {

    void onChatReceived(ChatDto dto);

    void onStateChanged(ConnectionState state);

    /** 최초 연결이 아니라 끊겼다 다시 붙은 경우에만 호출된다. 갭 복구 트리거. */
    void onReconnected();

    void onAuthFailure();

    /** 방 멤버 한 명의 읽음 커서가 올라갔다. lastReadChatId 는 그 사람이 읽은 마지막 chatId. */
    void onReadReceived(String roomId, String userId, long lastReadChatId);

    /**
     * 내가 멤버로 초대된 방이 새로 만들어졌다. 프레임에는 roomId 만 실려 온다 -
     * 나머지 필드(방 이름, 멤버 등)는 방 목록을 다시 조회해 채운다.
     */
    void onRoomCreated(String roomId);
}
