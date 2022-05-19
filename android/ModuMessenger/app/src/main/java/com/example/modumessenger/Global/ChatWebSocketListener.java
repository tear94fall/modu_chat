package com.example.modumessenger.Global;

import androidx.annotation.NonNull;

import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;

public class ChatWebSocketListener extends okhttp3.WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    @Override
    public void onOpen(WebSocket webSocket, @NonNull Response response) {
//        webSocket.close(NORMAL_CLOSURE_STATUS, "close");
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        System.out.println(text);

        JsonParser jsonParser = new JsonParser();
        Object object = jsonParser.parse(text);
        JSONObject jsonObject = (JSONObject) object;
        String msg = null;

        try {
            msg = (String) jsonObject.get("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }
}
