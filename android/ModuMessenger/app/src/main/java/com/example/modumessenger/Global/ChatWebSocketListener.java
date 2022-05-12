package com.example.modumessenger.Global;

import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

public class ChatWebSocketListener extends okhttp3.WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send("open");
        webSocket.close(NORMAL_CLOSURE_STATUS, "close");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }
}
