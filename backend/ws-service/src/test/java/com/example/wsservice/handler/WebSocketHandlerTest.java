package com.example.wsservice.handler;

import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.fcm.service.FcmService;
import com.example.wsservice.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketHandlerTest {

    private WebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketHandler(
                new ObjectMapper(),
                mock(ChatService.class),
                mock(ChatRoomService.class),
                mock(FcmService.class),
                mock(KafkaProducerService.class)
        );
        handler.getClients().clear();   // CLIENTS 가 static 이라 테스트 간 격리 필요
    }

    private WebSocketSession session(String userId, boolean open) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getUri()).thenReturn(URI.create("ws://localhost:8090/modu-chat"));
        HttpHeaders headers = new HttpHeaders();
        headers.add("userId", userId);
        when(s.getHandshakeHeaders()).thenReturn(headers);
        when(s.isOpen()).thenReturn(open);
        return s;
    }

    @Test
    @DisplayName("roomId 없는 경로로 접속해도 세션이 등록된다")
    void registersSessionWithoutRoomIdInPath() throws Exception {
        WebSocketSession s = session("user-a", true);

        handler.afterConnectionEstablished(s);

        assertThat(handler.getClients().get("user-a")).isSameAs(s);
    }

    @Test
    @DisplayName("같은 유저가 재연결하면 이전 세션을 닫는다")
    void closesPreviousSessionOnReconnect() throws Exception {
        WebSocketSession first = session("user-a", true);
        WebSocketSession second = session("user-a", true);

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        verify(first).close(CloseStatus.SESSION_NOT_RELIABLE);
        assertThat(handler.getClients().get("user-a")).isSameAs(second);
    }

    @Test
    @DisplayName("옛 세션의 close 이벤트가 새 세션을 제거하지 않는다")
    void staleCloseDoesNotEvictNewSession() throws Exception {
        WebSocketSession first = session("user-a", true);
        WebSocketSession second = session("user-a", true);

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.afterConnectionClosed(first, CloseStatus.SESSION_NOT_RELIABLE);

        assertThat(handler.getClients().get("user-a")).isSameAs(second);
    }

    @Test
    @DisplayName("자신의 세션이 닫히면 제거된다")
    void removesOwnSessionOnClose() throws Exception {
        WebSocketSession s = session("user-a", true);

        handler.afterConnectionEstablished(s);
        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        assertThat(handler.getClients()).doesNotContainKey("user-a");
    }

    @Test
    @DisplayName("이미 닫힌 이전 세션에는 close 를 호출하지 않는다")
    void doesNotCloseAlreadyClosedSession() throws Exception {
        WebSocketSession first = session("user-a", false);
        WebSocketSession second = session("user-a", true);

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        verify(first, never()).close(CloseStatus.SESSION_NOT_RELIABLE);
    }
}
