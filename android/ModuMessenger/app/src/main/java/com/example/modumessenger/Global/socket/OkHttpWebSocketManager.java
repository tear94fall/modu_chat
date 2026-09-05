package com.example.modumessenger.Global.socket;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.modumessenger.dto.ChatDto;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class OkHttpWebSocketManager implements WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final long PING_INTERVAL_SECONDS = 20L;
    private static final int NORMAL_CLOSURE = 1000;
    private static final int UNAUTHORIZED = 401;

    /** 매 연결 시점의 최신 자격증명을 읽기 위해 값이 아니라 공급자를 받는다. */
    public interface Credentials {
        String userId();
        String accessToken();
    }

    private final String wsUrl;
    private final Credentials credentials;
    private final ReconnectPolicy reconnectPolicy;
    private final Scheduler scheduler;
    private final NetworkMonitor networkMonitor;
    private final Gson gson = new Gson();
    private final OkHttpClient client;

    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile boolean intentionallyClosed = true;
    private volatile boolean everConnected = false;
    private volatile WebSocket webSocket;
    private volatile ChatSocketListener listener;

    /** openSocket/disconnect 마다 증가. 옛 소켓의 늦은 콜백을 구분하는 데 쓴다. */
    private volatile int generation = 0;

    public OkHttpWebSocketManager(String wsUrl,
                                  Credentials credentials,
                                  ReconnectPolicy reconnectPolicy,
                                  Scheduler scheduler,
                                  NetworkMonitor networkMonitor) {
        this.wsUrl = wsUrl;
        this.credentials = credentials;
        this.reconnectPolicy = reconnectPolicy;
        this.scheduler = scheduler;
        this.networkMonitor = networkMonitor;
        this.client = new OkHttpClient.Builder()
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    @Override
    public void setListener(ChatSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public ConnectionState getState() {
        return state;
    }

    @Override
    public synchronized void connect() {
        if (state == ConnectionState.CONNECTING || state == ConnectionState.CONNECTED) return;

        intentionallyClosed = false;
        scheduler.cancel();
        updateState(ConnectionState.CONNECTING);
        networkMonitor.start(this::onNetworkAvailable);
        openSocket();
    }

    @Override
    public synchronized void disconnect() {
        intentionallyClosed = true;
        generation++;   // 이후 도착하는 옛 소켓 콜백은 전부 stale 이 된다
        scheduler.cancel();
        networkMonitor.stop();
        reconnectPolicy.reset();

        WebSocket socket = webSocket;
        if (socket != null) {
            socket.close(NORMAL_CLOSURE, null);
            webSocket = null;
        }
        updateState(ConnectionState.DISCONNECTED);
    }

    @Override
    public boolean send(String payload) {
        WebSocket socket = webSocket;
        if (socket == null || state != ConnectionState.CONNECTED) return false;
        return socket.send(payload);
    }

    private synchronized void openSocket() {
        // disconnect() 와 같은 락을 잡는다. 예약된 재연결이나 네트워크 복구 콜백이
        // disconnect() 직후 뒤늦게 도착해도 여기서 걸러진다.
        if (intentionallyClosed) return;

        final int myGeneration = ++generation;

        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("userId", credentials.userId())
                .addHeader("Authorization", credentials.accessToken())
                .build();

        webSocket = client.newWebSocket(request, new SocketListener(myGeneration));
    }

    private synchronized void onNetworkAvailable() {
        if (intentionallyClosed) return;
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) return;

        Log.d(TAG, "network available, reconnecting immediately");
        scheduler.cancel();
        reconnectPolicy.reset();
        openSocket();
    }

    private void scheduleReconnect() {
        if (intentionallyClosed) return;

        updateState(ConnectionState.RECONNECTING);
        long delay = reconnectPolicy.nextDelayMs();
        Log.d(TAG, "reconnect scheduled in " + delay + "ms");
        scheduler.postDelayed(this::openSocket, delay);
    }

    private void updateState(ConnectionState next) {
        state = next;
        ChatSocketListener current = listener;
        if (current != null) current.onStateChanged(next);
    }

    private class SocketListener extends WebSocketListener {

        private final int generation;

        SocketListener(int generation) {
            this.generation = generation;
        }

        /** 이 리스너가 현재 소켓의 것이 아니면 콜백을 무시한다. */
        private boolean isStale() {
            return this.generation != OkHttpWebSocketManager.this.generation;
        }

        @Override
        public void onOpen(@NonNull WebSocket socket, @NonNull Response response) {
            if (isStale()) return;

            // 옛 콜백이 예약해 둔 재연결이 남아 있을 수 있다. 연결됐으니 취소한다.
            scheduler.cancel();

            boolean isReconnect = everConnected;
            everConnected = true;
            reconnectPolicy.reset();
            updateState(ConnectionState.CONNECTED);

            ChatSocketListener current = listener;
            if (current != null && isReconnect) current.onReconnected();
        }

        @Override
        public void onMessage(@NonNull WebSocket socket, @NonNull String text) {
            if (isStale()) return;

            ChatSocketListener current = listener;
            if (current == null) return;

            try {
                // 프로젝트가 고정한 gson 2.8.5 에는 정적 JsonParser.parseString 이 없다(2.8.9+).
                JsonObject json = new JsonParser().parse(text).getAsJsonObject();

                // READ / ROOM_CREATED 프레임은 ChatDto 모양이 아니다. 채팅 파싱보다 먼저 갈라낸다.
                if (json.has("type") && "READ".equals(json.get("type").getAsString())) {
                    current.onReadReceived(
                            json.get("roomId").getAsString(),
                            json.get("userId").getAsString(),
                            parseCursor(json.get("lastReadChatId")));
                    return;
                }

                if (json.has("type") && "ROOM_CREATED".equals(json.get("type").getAsString())) {
                    current.onRoomCreated(json.get("roomId").getAsString());
                    return;
                }

                ChatDto dto = gson.fromJson(text, ChatDto.class);
                if (dto != null) current.onChatReceived(dto);
            } catch (JsonSyntaxException | IllegalStateException | NullPointerException
                    | NumberFormatException e) {
                // lastReadChatId 는 서버가 String 타입으로 보낸다. "abc" 처럼 숫자로 못 읽는
                // 값이 오면 getAsLong() 이 NumberFormatException 을 던진다 — 소켓 리더 스레드가
                // 죽지 않도록 여기서 잡는다.
                Log.e(TAG, "malformed socket payload: " + text, e);
            }
        }

        /**
         * 읽음 커서를 읽는다. 서버는 이 값을 String 으로 내려주고,
         * 메시지가 하나도 없는 방은 빈 문자열이 온다. 숫자로 못 읽는 값은 0 으로 본다 —
         * 프레임을 통째로 버리면 그 방의 커서 이벤트를 놓친다.
         */
        private long parseCursor(JsonElement cursor) {
            if (cursor == null || cursor.isJsonNull()) return 0L;

            try {
                return Long.parseLong(cursor.getAsString().trim());
            } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException e) {
                return 0L;
            }
        }

        @Override
        public void onClosing(@NonNull WebSocket socket, int code, @NonNull String reason) {
            if (isStale()) return;

            // OkHttp 계약: 상대가 보낸 close 프레임에 응답해야 핸드셰이크가 끝나고
            // onClosed 가 도착한다. 응답하지 않으면 서버가 먼저 끊었을 때
            // 재연결이 예약되지 않고 ping 타임아웃까지 방치된다.
            socket.close(NORMAL_CLOSURE, null);
        }

        @Override
        public void onClosed(@NonNull WebSocket socket, int code, @NonNull String reason) {
            if (isStale()) return;

            if (intentionallyClosed) {
                updateState(ConnectionState.DISCONNECTED);
            } else {
                scheduleReconnect();
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket socket, @NonNull Throwable t, @Nullable Response response) {
            if (isStale()) return;

            Log.w(TAG, "socket failure", t);

            if (response != null && response.code() == UNAUTHORIZED) {
                ChatSocketListener current = listener;
                if (current != null) current.onAuthFailure();
            }
            scheduleReconnect();
        }
    }
}
