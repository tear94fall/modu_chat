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
}
