package com.example.modumessenger.common.handler;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private static List<WebSocketSession> list = new ArrayList<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        for(WebSocketSession sess: list) {
            sess.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        list.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        list.remove(session);
    }
}
