package com.example.modumessenger.Global.socket;

import com.example.modumessenger.dto.ChatDto;

public interface ChatSocketListener {

    void onChatReceived(ChatDto dto);

    void onStateChanged(ConnectionState state);

    /** 최초 연결이 아니라 끊겼다 다시 붙은 경우에만 호출된다. 갭 복구 트리거. */
    void onReconnected();

    void onAuthFailure();
}
