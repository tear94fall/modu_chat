package com.example.modumessenger.Global.socket;

public interface WebSocketManager {

    void connect();

    void disconnect();

    /** CONNECTED 상태가 아니면 false 를 반환하고 전송하지 않는다. */
    boolean send(String payload);

    ConnectionState getState();

    void setListener(ChatSocketListener listener);
}
