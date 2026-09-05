package com.example.wsservice.kafka.consumer;

import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.dto.SubscribeType;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.handler.WebSocketHandler;
import com.example.wsservice.member.dto.MemberDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaConsumerServiceTest {

    private static final String TOPIC = "topic-chat-room-created";

    private ChatService chatService;
    private ChatRoomService chatRoomService;
    private WebSocketHandler webSocketHandler;
    private ConcurrentHashMap<String, WebSocketSession> clients;
    private KafkaConsumerService kafkaConsumerService;
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        chatRoomService = mock(ChatRoomService.class);
        webSocketHandler = mock(WebSocketHandler.class);
        clients = new ConcurrentHashMap<>();
        when(webSocketHandler.getClients()).thenReturn(clients);
        acknowledgment = mock(Acknowledgment.class);

        kafkaConsumerService = new KafkaConsumerService(
                chatService, chatRoomService, new ObjectMapper(), webSocketHandler);
    }

    private MemberDto member(String userId) {
        return MemberDto.builder().userId(userId).build();
    }

    private ChatRoomDto roomWithMembers(String roomId, String... userIds) {
        return ChatRoomDto.builder()
                .roomId(roomId)
                .members(List.of(userIds).stream().map(this::member).toList())
                .build();
    }

    @Test
    @DisplayName("살아있는 세션에만 ROOM_CREATED 프레임을 보내고 없는 세션은 건너뛴다")
    void receiveRoomCreated_sendsOnlyToPresentSessions() throws IOException {
        WebSocketSession present = mock(WebSocketSession.class);
        clients.put("user-a", present);
        // user-b 는 접속해 있지 않다 - clients 맵에 없다.

        when(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"));

        ConsumerRecord<String, ChatMessage> record = new ConsumerRecord<>(
                TOPIC, 0, 0L, "room-1",
                new ChatMessage(SubscribeType.ROOM_CREATED, "room-1", null, null));

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment);

        verify(present).sendMessage(new TextMessage("{\"type\":\"ROOM_CREATED\",\"roomId\":\"room-1\"}"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("멤버 전원이 오프라인이면 아무 세션에도 보내지 않지만 ack 는 한다")
    void receiveRoomCreated_withNoLiveSessions_acknowledgesWithoutSending() {
        when(chatRoomService.getChatRoom("room-2")).thenReturn(roomWithMembers("room-2", "user-c"));

        ConsumerRecord<String, ChatMessage> record = new ConsumerRecord<>(
                TOPIC, 0, 0L, "room-2",
                new ChatMessage(SubscribeType.ROOM_CREATED, "room-2", null, null));

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment);

        assertThat(clients).isEmpty();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("방 조회가 실패해도 예외를 삼키고 ack 는 finally 에서 한다")
    void receiveRoomCreated_whenRoomLookupFails_stillAcknowledges() {
        when(chatRoomService.getChatRoom("missing-room")).thenThrow(new RuntimeException("not found"));

        ConsumerRecord<String, ChatMessage> record = new ConsumerRecord<>(
                TOPIC, 0, 0L, "missing-room",
                new ChatMessage(SubscribeType.ROOM_CREATED, "missing-room", null, null));

        kafkaConsumerService.receiveRoomCreated(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
