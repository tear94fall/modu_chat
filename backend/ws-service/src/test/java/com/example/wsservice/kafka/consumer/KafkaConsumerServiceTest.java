package com.example.wsservice.kafka.consumer;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.dto.ReactionSummaryDto;
import com.example.wsservice.chat.dto.SubscribeType;
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
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaConsumerServiceTest {

    private static final String TOPIC = "topic-chat-room-created";
    private static final String BROADCAST_TOPIC = "topic-chat-broadcast";

    private ObjectMapper objectMapper;
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
        objectMapper = new ObjectMapper();

        kafkaConsumerService = new KafkaConsumerService(
                chatService, chatRoomService, objectMapper, webSocketHandler);
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

    private ChatDto chat(String roomId) {
        return ChatDto.builder()
                .id(7L)
                .roomId(roomId)
                .sender("user-a")
                .message("hello")
                .chatType(1)
                .chatTime("2026-09-13 10:00:00")
                .build();
    }

    private ConsumerRecord<String, ChatMessage> broadcastRecord(String roomId, List<String> excludeUserIds) {
        return new ConsumerRecord<>(BROADCAST_TOPIC, 0, 0L, roomId,
                new ChatMessage(SubscribeType.BROAD_CAST, roomId, "7", null, excludeUserIds));
    }

    @Test
    @DisplayName("excludeUserIds 에 든 userId 의 세션에는 브로드캐스트하지 않는다")
    void receive_skipsExcludedSessions() throws IOException {
        WebSocketSession sender = mock(WebSocketSession.class);
        WebSocketSession blocked = mock(WebSocketSession.class);
        clients.put("user-a", sender);
        clients.put("user-b", blocked);

        ChatDto chatDto = chat("room-1");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"));
        when(chatService.getChat("7")).thenReturn(chatDto);

        kafkaConsumerService.receive(broadcastRecord("room-1", List.of("user-b")), acknowledgment);

        TextMessage expected = new TextMessage(objectMapper.writeValueAsString(chatDto));
        verify(sender).sendMessage(expected);
        verify(blocked, never()).sendMessage(any(TextMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("excludeUserIds 가 비어 있으면 멤버 전원에게 보낸다")
    void receive_withoutExclusion_sendsToEveryone() throws IOException {
        WebSocketSession a = mock(WebSocketSession.class);
        WebSocketSession b = mock(WebSocketSession.class);
        clients.put("user-a", a);
        clients.put("user-b", b);

        ChatDto chatDto = chat("room-1");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"));
        when(chatService.getChat("7")).thenReturn(chatDto);

        kafkaConsumerService.receive(broadcastRecord("room-1", null), acknowledgment);

        TextMessage expected = new TextMessage(objectMapper.writeValueAsString(chatDto));
        verify(a).sendMessage(expected);
        verify(b).sendMessage(expected);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("필드가 없는 옛 메시지(excludeUserIds=null)도 그대로 동작한다")
    void receive_legacyMessageWithoutField_stillBroadcasts() throws IOException {
        WebSocketSession a = mock(WebSocketSession.class);
        clients.put("user-a", a);

        ChatDto chatDto = chat("room-1");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"));
        when(chatService.getChat("7")).thenReturn(chatDto);

        ConsumerRecord<String, ChatMessage> record = new ConsumerRecord<>(BROADCAST_TOPIC, 0, 0L, "room-1",
                new ChatMessage(SubscribeType.BROAD_CAST, "room-1", "7", null));

        kafkaConsumerService.receive(record, acknowledgment);

        verify(a).sendMessage(new TextMessage(objectMapper.writeValueAsString(chatDto)));
        verify(acknowledgment).acknowledge();
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

    @Test
    @DisplayName("반응 토픽: 집계를 방 인원 전원에게 REACTION 프레임으로 보낸다(반응자 본인 포함)")
    void receiveReaction_sendsSummaryToEveryone() throws IOException {
        WebSocketSession a = mock(WebSocketSession.class);
        WebSocketSession b = mock(WebSocketSession.class);
        clients.put("user-a", a);
        clients.put("user-b", b);
        when(chatRoomService.getChatRoom("room-1")).thenReturn(roomWithMembers("room-1", "user-a", "user-b"));
        ChatMessage message = new ChatMessage(SubscribeType.REACTION, "room-1", "7", "user-b", null,
                "user-a", "LIKE", true, List.of(new ReactionSummaryDto("LIKE", 1, List.of("user-b"))));

        kafkaConsumerService.receiveReaction(new ConsumerRecord<>("topic-chat-reaction", 0, 0L, "room-1", message), acknowledgment);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(a).sendMessage(captor.capture());
        verify(b).sendMessage(any(TextMessage.class));
        String payload = captor.getValue().getPayload();
        org.assertj.core.api.Assertions.assertThat(payload).contains("\"type\":\"REACTION\"").contains("\"chatId\":\"7\"")
                .contains("\"emoji\":\"LIKE\"").contains("\"userIds\":[\"user-b\"]");
        verify(chatService, never()).getChat(any());
        verify(acknowledgment).acknowledge();
    }
}
