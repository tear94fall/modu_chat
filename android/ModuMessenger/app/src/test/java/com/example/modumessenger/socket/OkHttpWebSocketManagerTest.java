package com.example.modumessenger.socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.example.modumessenger.Global.socket.ChatSocketListener;
import com.example.modumessenger.Global.socket.ConnectionState;
import com.example.modumessenger.Global.socket.NetworkMonitor;
import com.example.modumessenger.Global.socket.OkHttpWebSocketManager;
import com.example.modumessenger.Global.socket.ReconnectPolicy;
import com.example.modumessenger.Global.socket.Scheduler;
import com.example.modumessenger.dto.ChatDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class OkHttpWebSocketManagerTest {

    private MockWebServer server;

    /** 지연 실행을 기록만 하고 자동 실행하지 않는다. 재연결 스케줄 여부를 검증하기 위함. */
    private static class RecordingScheduler implements Scheduler {
        final List<Long> delays = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch scheduled = new CountDownLatch(1);
        Runnable pending;

        @Override
        public synchronized void postDelayed(Runnable task, long delayMs) {
            delays.add(delayMs);
            pending = task;
            scheduled.countDown();
        }

        @Override
        public synchronized void cancel() {
            pending = null;
        }
    }

    private static class NoopNetworkMonitor implements NetworkMonitor {
        @Override public void start(Runnable onNetworkAvailable) { }
        @Override public void stop() { }
    }

    /**
     * OkHttp 계약: 상대의 close 프레임을 받으면 자기 쪽에서도 close 를 보내야
     * 핸드셰이크가 끝난다. 응답하지 않으면 소켓이 half-closed 로 남아
     * MockWebServer.shutdown() 이 executor 종료를 기다리다 타임아웃된다.
     */
    private static class AckingServerListener extends WebSocketListener {
        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            webSocket.close(code, reason);
        }
    }

    private static class CapturingListener implements ChatSocketListener {
        final BlockingQueue<ChatDto> received = new LinkedBlockingQueue<>();
        final BlockingQueue<ConnectionState> states = new LinkedBlockingQueue<>();
        final BlockingQueue<ReadEvent> readEvents = new LinkedBlockingQueue<>();
        final BlockingQueue<String> roomCreatedEvents = new LinkedBlockingQueue<>();
        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch reconnected = new CountDownLatch(1);
        volatile int reconnectedCount = 0;

        @Override public void onChatReceived(ChatDto dto) { received.add(dto); }

        @Override public void onStateChanged(ConnectionState state) {
            states.add(state);
            if (state == ConnectionState.CONNECTED) connected.countDown();
        }

        @Override public void onReconnected() {
            reconnectedCount++;
            reconnected.countDown();
        }

        @Override public void onAuthFailure() { }

        @Override public void onReadReceived(String roomId, String userId, long lastReadChatId) {
            readEvents.add(new ReadEvent(roomId, userId, lastReadChatId));
        }

        @Override public void onRoomCreated(String roomId) {
            roomCreatedEvents.add(roomId);
        }
    }

    /** onReadReceived 콜백 인자를 그대로 담아 큐에 넣기 위한 값 객체. */
    private static class ReadEvent {
        final String roomId;
        final String userId;
        final long lastReadChatId;

        ReadEvent(String roomId, String userId, long lastReadChatId) {
            this.roomId = roomId;
            this.userId = userId;
            this.lastReadChatId = lastReadChatId;
        }
    }

    private static final String SAMPLE_CHAT =
            "{\"id\":42,\"chatType\":0,\"roomId\":\"room-1\",\"sender\":\"user-b\","
                    + "\"message\":\"hello\",\"chatTime\":\"2026-08-26 10:00:00\"}";

    // 서버는 lastReadChatId 를 String 타입으로 보낸다 — 숫자 문자열도 파싱돼야 한다.
    private static final String SAMPLE_READ =
            "{\"type\":\"READ\",\"roomId\":\"room-1\",\"userId\":\"user-b\","
                    + "\"lastReadChatId\":\"315\"}";

    /** 메시지가 하나도 없는 방은 서버가 lastChatId 를 빈 문자열로 내려준다. */
    private static final String SAMPLE_READ_EMPTY_CURSOR =
            "{\"type\":\"READ\",\"roomId\":\"room-1\",\"userId\":\"user-b\","
                    + "\"lastReadChatId\":\"\"}";

    private static final String SAMPLE_ROOM_CREATED =
            "{\"type\":\"ROOM_CREATED\",\"roomId\":\"room-9\"}";

    private OkHttpWebSocketManager newManager(RecordingScheduler scheduler) {
        String wsUrl = server.url("/ws-service/modu-chat").toString().replaceFirst("^http", "ws");

        return new OkHttpWebSocketManager(
                wsUrl,
                new OkHttpWebSocketManager.Credentials() {
                    @Override public String userId() { return "user-a"; }
                    @Override public String accessToken() { return "test-token"; }
                },
                new ReconnectPolicy(),
                scheduler,
                new NoopNetworkMonitor());
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void reachesConnectedStateOnHandshake() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        assertTrue(listener.connected.await(5, TimeUnit.SECONDS));
        assertEquals(ConnectionState.CONNECTED, manager.getState());
    }

    @Test
    public void sendsHandshakeHeaders() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();
        assertTrue(listener.connected.await(5, TimeUnit.SECONDS));

        okhttp3.mockwebserver.RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("user-a", request.getHeader("userId"));
        assertEquals("test-token", request.getHeader("Authorization"));
        assertEquals("/ws-service/modu-chat", request.getPath());
    }

    @Test
    public void parsesIncomingChat() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.send(SAMPLE_CHAT);
            }
        }));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        ChatDto dto = listener.received.poll(5, TimeUnit.SECONDS);
        assertNotNull(dto);
        assertEquals(Long.valueOf(42L), dto.getId());
        assertEquals("room-1", dto.getRoomId());
        assertEquals("hello", dto.getMessage());
    }

    @Test
    public void parsesIncomingReadFrame_withStringLastReadChatId() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.send(SAMPLE_READ);
            }
        }));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        ReadEvent event = listener.readEvents.poll(5, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals("room-1", event.roomId);
        assertEquals("user-b", event.userId);
        assertEquals(315L, event.lastReadChatId);
    }

    @Test
    public void parsesReadFrame_withEmptyCursor_asZero() throws Exception {
        // 빈 방에 들어가면 lastReadChatId 가 "" 로 온다. 예외로 프레임을 통째로
        // 버리면 그 방의 커서 이벤트를 놓친다 — 0 으로 읽는다.
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.send(SAMPLE_READ_EMPTY_CURSOR);
            }
        }));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        ReadEvent event = listener.readEvents.poll(5, TimeUnit.SECONDS);
        assertNotNull("빈 커서 프레임도 버리지 않는다", event);
        assertEquals("room-1", event.roomId);
        assertEquals(0L, event.lastReadChatId);
    }

    @Test
    public void parsesIncomingRoomCreatedFrame() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.send(SAMPLE_ROOM_CREATED);
            }
        }));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        String roomId = listener.roomCreatedEvents.poll(5, TimeUnit.SECONDS);
        assertNotNull(roomId);
        assertEquals("room-9", roomId);
    }

    @Test
    public void schedulesReconnectOnHandshakeFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        // 상태가 아니라 "예약되었음"을 기다린다. updateState 가 postDelayed 보다 먼저
        // 실행되므로, 이 래치가 열리면 상태는 이미 RECONNECTING 이다 (happens-before 성립).
        assertTrue("재연결이 예약되지 않았다", scheduler.scheduled.await(5, TimeUnit.SECONDS));
        assertFalse(scheduler.delays.isEmpty());
        assertEquals(ConnectionState.RECONNECTING, manager.getState());
    }

    @Test
    public void doesNotReconnectAfterIntentionalDisconnect() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();
        assertTrue(listener.connected.await(5, TimeUnit.SECONDS));

        manager.disconnect();
        Thread.sleep(500L);

        assertTrue(scheduler.delays.isEmpty());
        assertEquals(ConnectionState.DISCONNECTED, manager.getState());
    }

    @Test
    public void sendFailsWhenNotConnected() {
        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);

        assertFalse(manager.send("{}"));
    }

    @Test
    public void pendingReconnectDoesNotOpenSocketAfterDisconnect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();
        assertTrue(scheduler.scheduled.await(5, TimeUnit.SECONDS));

        // 이미 큐에서 꺼내진 재연결 작업. scheduler.cancel() 로는 막을 수 없는 상황을 모사한다.
        Runnable pendingReconnect = scheduler.pending;
        assertNotNull(pendingReconnect);

        // 재연결이 잘못 실행되면 이 응답을 소비하고 CONNECTED 가 된다.
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        manager.disconnect();
        pendingReconnect.run();

        assertFalse("disconnect 이후에는 재연결되면 안 된다",
                listener.connected.await(2, TimeUnit.SECONDS));
        assertEquals(ConnectionState.DISCONNECTED, manager.getState());
    }

    @Test
    public void serverInitiatedCloseSchedulesExactlyOneReconnect() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.close(1001, "going away");
            }
        }));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();

        assertTrue("서버가 끊었으면 재연결이 예약되어야 한다",
                scheduler.scheduled.await(5, TimeUnit.SECONDS));
        assertEquals("재연결 예약은 정확히 한 번이어야 한다", 1, scheduler.delays.size());
    }

    @Test
    public void reconnectNotifiesListener() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                webSocket.close(1001, "going away");
            }
        }));
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();
        assertTrue(scheduler.scheduled.await(5, TimeUnit.SECONDS));

        Runnable pending = scheduler.pending;
        assertNotNull(pending);
        pending.run();   // 예약된 재연결을 직접 실행

        assertTrue("재연결 후 onReconnected 가 호출되어야 한다",
                listener.reconnected.await(5, TimeUnit.SECONDS));
        assertEquals(1, listener.reconnectedCount);
        assertEquals(ConnectionState.CONNECTED, manager.getState());
    }

    @Test
    public void staleCallbackAfterReconnectCycleDoesNotScheduleReconnect() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));
        server.enqueue(new MockResponse().withWebSocketUpgrade(new AckingServerListener()));

        RecordingScheduler scheduler = new RecordingScheduler();
        OkHttpWebSocketManager manager = newManager(scheduler);
        CapturingListener listener = new CapturingListener();
        manager.setListener(listener);

        manager.connect();
        assertTrue(listener.connected.await(5, TimeUnit.SECONDS));

        // disconnect 는 graceful close 라 옛 소켓의 onClosed 가 늦게 도착한다.
        // 그 사이에 다시 connect 하면, 가드가 없을 때 옛 콜백이 재연결을 예약해 버린다.
        manager.disconnect();
        manager.connect();

        Thread.sleep(1500L);

        assertTrue("옛 소켓의 늦은 콜백이 재연결을 예약해서는 안 된다",
                scheduler.delays.isEmpty());
        assertEquals(ConnectionState.CONNECTED, manager.getState());
    }
}
