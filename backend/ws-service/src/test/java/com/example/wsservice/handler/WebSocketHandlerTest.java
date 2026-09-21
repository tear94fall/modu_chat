package com.example.wsservice.handler;

import com.example.wsservice.block.BlockRelationCache;
import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatMessage;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.chat.dto.ReactionResultDto;
import com.example.wsservice.chat.dto.ReactionSummaryDto;
import com.example.wsservice.fcm.dto.FcmUserMessageDto;
import com.example.wsservice.chat.dto.SubscribeType;
import com.example.wsservice.chat.service.ChatRoomService;
import com.example.wsservice.chat.service.ChatService;
import com.example.wsservice.fcm.dto.FcmMessageDto;
import com.example.wsservice.fcm.service.FcmService;
import com.example.wsservice.kafka.producer.KafkaProducerService;
import com.example.wsservice.member.dto.MemberDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketHandlerTest {

    private WebSocketHandler handler;
    private ObjectMapper objectMapper;
    private ChatService chatService;
    private ChatRoomService chatRoomService;
    private FcmService fcmService;
    private KafkaProducerService kafkaProducerService;
    private BlockRelationCache blockRelationCache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatService = mock(ChatService.class);
        chatRoomService = mock(ChatRoomService.class);
        fcmService = mock(FcmService.class);
        kafkaProducerService = mock(KafkaProducerService.class);
        blockRelationCache = mock(BlockRelationCache.class);

        handler = new WebSocketHandler(
                objectMapper,
                chatService,
                chatRoomService,
                fcmService,
                kafkaProducerService,
                blockRelationCache
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

    private MemberDto member(String userId) {
        return MemberDto.builder().userId(userId).username(userId).build();
    }

    private ChatRoomDto room(String roomId, String... userIds) {
        return ChatRoomDto.builder()
                .roomId(roomId)
                .roomName(roomId)
                .members(Arrays.stream(userIds).map(this::member).toList())
                .build();
    }

    private TextMessage chatFrame(String roomId, String sender) throws Exception {
        ChatDto chatDto = new ChatDto("hello", roomId, sender, "2026-09-13 10:00:00", 1, null);
        return new TextMessage(objectMapper.writeValueAsString(chatDto));
    }

    private ChatMessage capturedBroadcast(String roomId) {
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(kafkaProducerService).sendMessage(eq(roomId), captor.capture());
        return captor.getValue();
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

    @Test
    @DisplayName("1:1 방에서 상대가 나를 차단했으면 excludeUserIds 에 상대가 실리고 푸시는 보내지 않는다")
    void oneToOneBlocked_excludesOtherAndSkipsFcm() throws Exception {
        ChatRoomDto chatRoomDto = room("room-1", "user-a", "user-b");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto);
        when(chatService.saveChat(any())).thenReturn(7L);
        when(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto);
        when(blockRelationCache.blockedBy("user-a")).thenReturn(Set.of("user-b"));

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"));

        ChatMessage sent = capturedBroadcast("room-1");
        assertThat(sent.getType()).isEqualTo(SubscribeType.BROAD_CAST);
        assertThat(sent.getChatId()).isEqualTo("7");
        assertThat(sent.getExcludeUserIds()).containsExactly("user-b");

        // 발신자에게는 정상으로 보여야 하므로 저장·lastChat 갱신은 그대로 한다
        verify(chatService).saveChat(any());
        verify(chatRoomService).updateChatRoom(eq("room-1"), any());
        assertThat(chatRoomDto.getLastChatId()).isEqualTo("7");

        verify(fcmService, never()).sendFcmMessage(any());
    }

    @Test
    @DisplayName("1:1 방에서 차단이 없으면 제외 없이 기존대로 푸시까지 간다")
    void oneToOneNotBlocked_behavesAsBefore() throws Exception {
        ChatRoomDto chatRoomDto = room("room-1", "user-a", "user-b");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto);
        when(chatService.saveChat(any())).thenReturn(8L);
        when(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto);
        when(blockRelationCache.blockedBy("user-a")).thenReturn(Set.of());

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"));

        assertThat(capturedBroadcast("room-1").getExcludeUserIds()).isNullOrEmpty();
        verify(fcmService).sendFcmMessage(any(FcmMessageDto.class));
    }

    @Test
    @DisplayName("푸시 전송이 실패해도 세션을 닫지 않는다 - 저장과 브로드캐스트는 이미 끝났으므로 예외를 삼킨다")
    void fcmFailure_doesNotPropagate() throws Exception {
        ChatRoomDto chatRoomDto = room("room-1", "user-a", "user-b");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto);
        when(chatService.saveChat(any())).thenReturn(9L);
        when(chatRoomService.updateChatRoom(eq("room-1"), any())).thenReturn(chatRoomDto);
        when(blockRelationCache.blockedBy("user-a")).thenReturn(Set.of());
        org.mockito.Mockito.doThrow(new RuntimeException("push-service 500"))
                .when(fcmService).sendFcmMessage(any(FcmMessageDto.class));

        WebSocketSession session = session("user-a", true);

        // 예외가 새어 나가면 ExceptionWebSocketHandlerDecorator 가 발신자 세션을 닫고 에코가 유실된다
        handler.handleTextMessage(session, chatFrame("room-1", "user-a"));

        assertThat(capturedBroadcast("room-1").getChatId()).isEqualTo("9");
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("단체방은 차단이 있어도 제외하지 않고 푸시도 그대로 보낸다")
    void groupRoomBlocked_doesNotExclude() throws Exception {
        ChatRoomDto chatRoomDto = room("room-2", "user-a", "user-b", "user-c");
        when(chatRoomService.getChatRoom("room-2")).thenReturn(chatRoomDto);
        when(chatService.saveChat(any())).thenReturn(9L);
        when(chatRoomService.updateChatRoom(eq("room-2"), any())).thenReturn(chatRoomDto);

        handler.handleTextMessage(session("user-a", true), chatFrame("room-2", "user-a"));

        assertThat(capturedBroadcast("room-2").getExcludeUserIds()).isNullOrEmpty();
        verify(fcmService).sendFcmMessage(any(FcmMessageDto.class));
        verify(blockRelationCache, never()).blockedBy(anyString());
    }

    @Test
    @DisplayName("READ 프레임은 차단 조회 없이 기존 흐름 그대로다")
    void readFrame_isUnchanged() throws Exception {
        ChatRoomDto chatRoomDto = room("room-1", "user-a", "user-b");
        chatRoomDto.setLastChatId("11");
        when(chatRoomService.getChatRoom("room-1")).thenReturn(chatRoomDto);

        TextMessage frame = new TextMessage("{\"type\":\"READ\",\"roomId\":\"room-1\",\"sender\":\"user-b\"}");

        handler.handleTextMessage(session("user-b", true), frame);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(kafkaProducerService).sendReadMessage(eq("room-1"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SubscribeType.READ);
        assertThat(captor.getValue().getChatId()).isEqualTo("11");
        assertThat(captor.getValue().getExcludeUserIds()).isNull();

        verify(blockRelationCache, never()).blockedBy(anyString());
        verify(kafkaProducerService, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("차단 대상 목록이 붙어도 방 멤버가 아니면 아무것도 하지 않는다")
    void nonMemberSender_isIgnored() throws Exception {
        when(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-b", "user-c"));

        handler.handleTextMessage(session("user-a", true), chatFrame("room-1", "user-a"));

        verify(chatService, never()).saveChat(any());
        verify(kafkaProducerService, never()).sendMessage(anyString(), any());
        verify(fcmService, never()).sendFcmMessage(any());
    }

    @Test
    @DisplayName("excludeUserIds 가 없는 ChatMessage 는 기존 JSON 모양 그대로 직렬화된다")
    void chatMessageWithoutExclusion_keepsLegacyJsonShape() throws Exception {
        String json = objectMapper.writeValueAsString(
                new ChatMessage(SubscribeType.BROAD_CAST, "room-1", "7", null));

        assertThat(json).isEqualTo("{\"type\":\"BROAD_CAST\",\"roomId\":\"room-1\",\"chatId\":\"7\"}");

        // 필드가 없는 옛 메시지도 그대로 읽힌다
        ChatMessage legacy = objectMapper.readValue(
                "{\"type\":\"BROAD_CAST\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"userId\":null}", ChatMessage.class);
        assertThat(legacy.getExcludeUserIds()).isNull();

        ChatMessage withExclusion = objectMapper.readValue(
                objectMapper.writeValueAsString(
                        new ChatMessage(SubscribeType.BROAD_CAST, "room-1", "7", null, List.of("user-b"))),
                ChatMessage.class);
        assertThat(withExclusion.getExcludeUserIds()).containsExactly("user-b");
    }

    @Test
    @DisplayName("REACTION 프레임: chat-service 결과를 반응 토픽에 싣고, 남겨졌으면 작성자에게만 푸시한다")
    void reactionFrame_broadcastsAndPushesAuthor() throws Exception {
        when(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"));
        ReactionResultDto result = ReactionResultDto.builder()
                .chatId(7L).roomId("room-1").authorUserId("user-a").added(true).emoji("LIKE")
                .reactions(List.of(new ReactionSummaryDto("LIKE", 1, List.of("user-b"))))
                .build();
        when(chatService.react("room-1", "7", "user-b", "LIKE")).thenReturn(result);

        TextMessage frame = new TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-b\",\"emoji\":\"LIKE\"}");
        handler.handleTextMessage(session("user-b", true), frame);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(kafkaProducerService).sendReactionMessage(eq("room-1"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(SubscribeType.REACTION);
        assertThat(captor.getValue().getChatId()).isEqualTo("7");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-b");
        assertThat(captor.getValue().getReactions()).hasSize(1);

        ArgumentCaptor<FcmUserMessageDto> push = ArgumentCaptor.forClass(FcmUserMessageDto.class);
        verify(fcmService).sendUserMessage(push.capture());
        assertThat(push.getValue().getUserId()).isEqualTo("user-a");
        assertThat(push.getValue().getBody()).isEqualTo("user-b님이 👍 반응을 남겼습니다");
        assertThat(push.getValue().getData()).containsEntry("roomId", "room-1").containsEntry("sender", "user-b").containsEntry("kind", "REACTION");
        verify(kafkaProducerService, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("반응 취소는 브로드캐스트만 하고 푸시하지 않는다")
    void reactionRemoval_doesNotPush() throws Exception {
        when(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"));
        when(chatService.react("room-1", "7", "user-b", "LIKE")).thenReturn(ReactionResultDto.builder()
                .chatId(7L).roomId("room-1").authorUserId("user-a").added(false).reactions(List.of()).build());

        handler.handleTextMessage(session("user-b", true),
                new TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-b\",\"emoji\":\"LIKE\"}"));

        verify(kafkaProducerService).sendReactionMessage(eq("room-1"), any());
        verify(fcmService, never()).sendUserMessage(any());
    }

    @Test
    @DisplayName("chat-service 가 거부하면(내 메시지 등) 아무것도 보내지 않고 세션도 살아 있다")
    void reactionRejected_isDropped() throws Exception {
        when(chatRoomService.getChatRoom("room-1")).thenReturn(room("room-1", "user-a", "user-b"));
        when(chatService.react("room-1", "7", "user-a", "LIKE")).thenThrow(new RuntimeException("400 own chat"));

        handler.handleTextMessage(session("user-a", true),
                new TextMessage("{\"type\":\"REACTION\",\"roomId\":\"room-1\",\"chatId\":\"7\",\"sender\":\"user-a\",\"emoji\":\"LIKE\"}"));

        verify(kafkaProducerService, never()).sendReactionMessage(anyString(), any());
        verify(fcmService, never()).sendUserMessage(any());
    }
}
