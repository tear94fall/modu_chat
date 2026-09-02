package com.example.modumessenger.Repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Global.SingleLiveEvent;
import com.example.modumessenger.Global.socket.ChatSocketListener;
import com.example.modumessenger.Global.socket.ConnectionState;
import com.example.modumessenger.Global.socket.WebSocketManager;
import com.example.modumessenger.Retrofit.APIHelper;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatReadCursorDto;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.ChatRoomUnreadDto;
import com.example.modumessenger.entity.ChatRoom;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 소켓 / REST 를 하나의 LiveData 로 합치는 단일 진실 원천.
 * 메시지가 어디서 왔는지를 UI 가 알 필요 없게 만드는 것이 목적이다.
 */
public class ChatRepository implements ChatSocketListener {

    private static final String TAG = "ChatRepository";

    /** 재연결 후 갭 복구로 다시 받아올 최신 채팅 개수. */
    public static final int GAP_RECOVERY_SIZE = 30;

    private final WebSocketManager webSocketManager;
    private final RetrofitChatAPI chatApi;
    private final RetrofitChatRoomAPI chatRoomApi;
    private final Gson gson = new Gson();

    private final MutableLiveData<List<ChatBubble>> activeRoomChats =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ChatRoom>> chatRooms =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ConnectionState> connectionState =
            new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final SingleLiveEvent<BannerEvent> banner = new SingleLiveEvent<>();

    /** chatId -> bubble. 삽입 순서를 유지하면서 chatId 로 중복을 제거한다. */
    private final LinkedHashMap<Long, ChatBubble> activeRoomIndex = new LinkedHashMap<>();

    /** LiveData.getValue() 는 postValue 직후 비동기라 신뢰할 수 없어 별도 캐시를 둔다. */
    private final List<ChatRoom> roomCache = new ArrayList<>();

    /**
     * 하단 탭 배지가 쓰는 안 읽음 총합.
     * chatRooms 에서 파생시키면 서버 병합·실시간 증가·읽음 소거가 모두 자동으로 반영된다.
     * Transformations.map 은 호출할 때마다 새 LiveData 를 만들어 필드로 한 번만 둔다.
     */
    private final LiveData<Integer> totalUnreadCount =
            Transformations.map(chatRooms, rooms -> {
                int total = 0;
                for (ChatRoom room : rooms) {
                    total += Math.max(0, room.getUnreadCount());
                }
                return total;
            });

    private volatile String activeRoomId = null;
    private volatile String myUserId = "";
    private volatile String myMemberId = null;

    /** 활성 방의 멤버별 읽음 커서. 키 집합이 곧 방 멤버 목록이다. */
    private final Map<String, Long> readCursors = new HashMap<>();

    /**
     * 소켓이 끊겨 있어 전달하지 못한 READ 의 방 목록. 재연결 때 다시 보낸다.
     * closeRoom 의 REST 폴백은 즉시 3회 재시도로 끝나 오프라인 구간을 못 넘긴다.
     */
    private final Set<String> pendingReadRooms = new LinkedHashSet<>();

    public ChatRepository(WebSocketManager webSocketManager,
                          RetrofitChatAPI chatApi,
                          RetrofitChatRoomAPI chatRoomApi) {
        this.webSocketManager = webSocketManager;
        this.chatApi = chatApi;
        this.chatRoomApi = chatRoomApi;
        this.webSocketManager.setListener(this);
    }

    public void setIdentity(String userId, String memberId) {
        this.myUserId = userId == null ? "" : userId;
        this.myMemberId = memberId;
    }

    public LiveData<List<ChatBubble>> getActiveRoomChats() { return activeRoomChats; }

    public LiveData<List<ChatRoom>> getChatRooms() { return chatRooms; }

    public LiveData<Integer> getTotalUnreadCount() { return totalUnreadCount; }

    public LiveData<ConnectionState> getConnectionState() { return connectionState; }

    public SingleLiveEvent<BannerEvent> getBanner() { return banner; }

    public void clearBanner() {
        banner.clearPending();
    }

    public String getActiveRoomId() { return activeRoomId; }

    public void openRoom(String roomId) {
        synchronized (activeRoomIndex) {
            activeRoomId = roomId;
            activeRoomIndex.clear();
            readCursors.clear();
            activeRoomChats.postValue(new ArrayList<>());
        }
        // 서버 반영은 closeRoom 에서 한다. 여기서는 체감을 위해 로컬만 0 으로.
        clearUnread(roomId);
        refreshReadCursors(roomId);
        sendReadReceipt(roomId);
    }

    public void closeRoom(String roomId) {
        String memberId = myMemberId;

        // clearUnread 와 서버 호출은 이 블록 밖에 둔다. clearUnread 가 roomCache 락을 잡으므로
        // 안쪽에 두면 handleChat(roomCache -> activeRoomIndex)과 반대 순서로 두 락이 겹친다.
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;

            activeRoomId = null;
            activeRoomIndex.clear();
            readCursors.clear();
            activeRoomChats.postValue(new ArrayList<>());
        }

        clearUnread(roomId);
        sendReadReceipt(roomId);

        if (memberId == null) return;
        APIHelper.enqueueWithRetry(chatRoomApi.RequestUpdateLastRead(roomId, memberId),
                new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "updateLastRead failed, code=" + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        // 실패해도 앱 동작을 막지 않는다. 다음 방 목록 갱신에서
                        // 서버의 옛 값이 돌아와 배지가 되살아난다.
                        Log.e(TAG, "updateLastRead failed", t);
                    }
                });
    }

    public boolean sendChat(ChatDto dto) {
        return webSocketManager.send(gson.toJson(dto));
    }

    /**
     * "여기까지 읽었다" 를 소켓으로 알린다.
     * 서버가 커서를 lastChatId 로 점프시키고 방 인원에게 브로드캐스트한다.
     * 끊겨 있어 못 보냈으면 방을 적어 두고 재연결 때 다시 보낸다.
     */
    private void sendReadReceipt(String roomId) {
        if (roomId == null || myUserId.isEmpty()) return;

        Map<String, String> frame = new HashMap<>();
        frame.put("type", "READ");
        frame.put("roomId", roomId);
        frame.put("sender", myUserId);

        boolean delivered = webSocketManager.send(gson.toJson(frame));
        synchronized (pendingReadRooms) {
            if (delivered) {
                pendingReadRooms.remove(roomId);
            } else {
                pendingReadRooms.add(roomId);
            }
        }
    }

    /**
     * 끊긴 동안 못 보낸 READ 를 다시 보낸다.
     * 서버는 커서를 그 시점의 lastChatId 로 점프시키므로, 끊긴 동안 쌓인 메시지까지
     * 읽은 것이 된다 — closeRoom 의 REST 폴백이 성공했을 때와 같은 결과다.
     */
    private void flushPendingReadReceipts() {
        List<String> rooms;
        synchronized (pendingReadRooms) {
            if (pendingReadRooms.isEmpty()) return;
            rooms = new ArrayList<>(pendingReadRooms);
        }
        for (String roomId : rooms) {
            sendReadReceipt(roomId);
        }
    }

    // ---------- REST ----------

    public void loadInitialChats(String roomId, int size) {
        APIHelper.enqueueWithRetry(
                chatApi.RequestChatListSize(roomId, String.valueOf(size)),
                chatListCallback(roomId, false));
    }

    public void loadPrevChats(String roomId, String lastChatId, int size) {
        APIHelper.enqueueWithRetry(
                chatApi.RequestPrevChatList(roomId, lastChatId, String.valueOf(size)),
                chatListCallback(roomId, true));
    }

    public void refreshChatRooms() {
        String memberId = myMemberId;
        if (memberId == null) return;

        APIHelper.enqueueWithRetry(chatRoomApi.RequestChatRooms(memberId),
                new Callback<List<ChatRoomDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ChatRoomDto>> call,
                                           @NonNull Response<List<ChatRoomDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        applyChatRoomDtos(response.body());
                        // 방 목록이 채워진 뒤에 병합해야 roomCache 에 반영된다.
                        refreshUnreadCounts();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                        Log.e(TAG, "refreshChatRooms failed", t);
                    }
                });
    }

    public void applyChatRoomDtos(List<ChatRoomDto> dtos) {
        List<ChatRoom> rooms = new ArrayList<>();
        for (ChatRoomDto dto : dtos) {
            rooms.add(new ChatRoom(dto));
        }
        setChatRooms(rooms);
    }

    public void setChatRooms(List<ChatRoom> rooms) {
        synchronized (roomCache) {
            roomCache.clear();
            roomCache.addAll(rooms);
            chatRooms.postValue(new ArrayList<>(roomCache));
        }
    }

    /**
     * 안 읽은 개수를 서버에서 받아 roomCache 에 병합한다.
     * 방 목록과 별개의 호출이므로, 실패해도 방 목록 자체는 그대로 남는다.
     */
    public void refreshUnreadCounts() {
        String memberId = myMemberId;
        if (memberId == null) return;

        APIHelper.enqueueWithRetry(chatRoomApi.RequestUnreadCounts(memberId),
                new Callback<List<ChatRoomUnreadDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ChatRoomUnreadDto>> call,
                                           @NonNull Response<List<ChatRoomUnreadDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        applyUnreadCounts(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ChatRoomUnreadDto>> call,
                                          @NonNull Throwable t) {
                        // 배지만 비고 방 목록은 정상이다. 다음 갱신에서 다시 시도된다.
                        Log.e(TAG, "refreshUnreadCounts failed", t);
                    }
                });
    }

    void applyUnreadCounts(List<ChatRoomUnreadDto> dtos) {
        synchronized (roomCache) {
            Map<String, Integer> byRoom = new HashMap<>();
            for (ChatRoomUnreadDto dto : dtos) {
                if (dto.getRoomId() == null) continue;
                Long count = dto.getUnreadChatCount();
                byRoom.put(dto.getRoomId(), count == null ? 0 : count.intValue());
            }

            for (ChatRoom room : roomCache) {
                // 응답에 없는 방은 0 으로 둔다. 서버가 진실이다.
                Integer count = byRoom.get(room.getRoomId());
                room.setUnreadCount(count == null ? 0 : count);
            }
            chatRooms.postValue(new ArrayList<>(roomCache));
        }
    }

    /** 로컬에서만 +1 한다. 서버 재조회 없이 배지가 즉시 반응하게 하기 위해서다. */
    void incrementUnread(String roomId) {
        if (roomId == null) return;
        synchronized (roomCache) {
            for (ChatRoom room : roomCache) {
                if (!roomId.equals(room.getRoomId())) continue;
                room.setUnreadCount(room.getUnreadCount() + 1);
                chatRooms.postValue(new ArrayList<>(roomCache));
                return;
            }
        }
    }

    /** 로컬에서만 0 으로 만든다. 서버 반영은 closeRoom 에서 한다. */
    void clearUnread(String roomId) {
        if (roomId == null) return;
        synchronized (roomCache) {
            for (ChatRoom room : roomCache) {
                if (!roomId.equals(room.getRoomId())) continue;
                if (room.getUnreadCount() == 0) return;
                room.setUnreadCount(0);
                chatRooms.postValue(new ArrayList<>(roomCache));
                return;
            }
        }
    }

    private Callback<List<ChatDto>> chatListCallback(String roomId, boolean prepend) {
        return new Callback<List<ChatDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatDto>> call,
                                   @NonNull Response<List<ChatDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                mergeChats(roomId, response.body(), prepend);
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "chat list load failed for " + roomId, t);
            }
        };
    }

    /**
     * chatId 를 아직 안 읽은 방 인원 수.
     * 발신자 본인은 언제나 읽은 것으로 본다 — 보냈다는 것이 읽었다는 뜻이다.
     * 커서가 없거나 null 인 멤버는 한 번도 안 읽은 것으로 센다.
     */
    static int unreadCountFor(long chatId, String senderUserId, Map<String, Long> cursors) {
        int count = 0;
        for (Map.Entry<String, Long> entry : cursors.entrySet()) {
            if (senderUserId != null && senderUserId.equals(entry.getKey())) continue;

            Long cursor = entry.getValue();
            if (cursor == null || cursor < chatId) count++;
        }
        return count;
    }

    /**
     * 메시지 자체에서 유도한 읽음 하한을 커서 맵에 얹는다.
     * 누군가 id N 을 보냈다는 건 그 방을 보고 있었다는 뜻이므로 N 까지는 읽은 것이다.
     * READ 프레임이 유실되거나 커서가 NULL 로 남은 방에서도 숫자가 맞게 된다.
     *
     * 나는 예외다. 이 계산은 활성 방에서만 도는데, 그 방은 내가 지금 보고 있는 방이고
     * openRoom 이 이미 서버에 READ 를 보냈다. 그러니 내 커서는 내가 보낸 마지막 메시지가
     * 아니라 화면의 마지막 메시지까지다 — 방에 처음 들어간 순간 GET 스냅샷이 옛 커서를
     * 들고 와도 상대 메시지에 숫자가 남지 않는다.
     *
     * 커서를 올리기만 하고, 맵에 없는 발신자는 건너뛴다 — 키 집합이 곧 분모라
     * 여기서 키를 늘리면 방 인원을 잘못 세게 된다.
     */
    static Map<String, Long> withImpliedCursors(Map<String, Long> cursors,
                                                Map<Long, ChatBubble> bubblesById,
                                                String myUserId) {
        Map<String, Long> merged = new HashMap<>(cursors);

        for (Map.Entry<Long, ChatBubble> entry : bubblesById.entrySet()) {
            Long chatId = entry.getKey();
            if (chatId == null) continue;

            raise(merged, entry.getValue().getSender(), chatId);
            raise(merged, myUserId, chatId);
        }
        return merged;
    }

    /** 맵에 이미 있는 사용자의 커서만, 더 큰 값으로만 올린다. */
    private static void raise(Map<String, Long> cursors, String userId, long chatId) {
        if (userId == null || !cursors.containsKey(userId)) return;

        Long current = cursors.get(userId);
        if (current == null || current < chatId) {
            cursors.put(userId, chatId);
        }
    }

    /** 방에 들어갈 때 서버에서 커서를 받아온다. 실패하면 숫자가 안 뜰 뿐 대화는 정상이다. */
    public void refreshReadCursors(String roomId) {
        if (roomId == null) return;

        APIHelper.enqueueWithRetry(chatRoomApi.RequestReadCursors(roomId),
                new Callback<List<ChatReadCursorDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ChatReadCursorDto>> call,
                                           @NonNull Response<List<ChatReadCursorDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;

                        Map<String, Long> cursors = new HashMap<>();
                        for (ChatReadCursorDto dto : response.body()) {
                            if (dto.getUserId() == null) continue;
                            cursors.put(dto.getUserId(), dto.getLastReadChatId());
                        }
                        applyReadCursors(roomId, cursors);
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ChatReadCursorDto>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "refreshReadCursors failed for " + roomId, t);
                    }
                });
    }

    /** 커서를 병합하고 활성 방 말풍선을 다시 계산한다. */
    void applyReadCursors(String roomId, Map<String, Long> cursors) {
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;

            // openRoom 이 이 GET 요청과 READ 프레임을 연달아 보낸다. 소켓 브로드캐스트(최신)가
            // GET 응답(스냅샷)보다 먼저 도착하는 경우가 흔해서, 통째로 갈아끼우면 방금 반영된
            // 최신 커서가 옛 스냅샷 값으로 되돌아간다. 키별로 기존 값과 응답 값 중 큰 쪽을 쓰되,
            // 응답에 없는 멤버(방을 나간 사람)는 제거한다 — 멤버 목록은 응답이 진실이다.
            Map<String, Long> merged = new HashMap<>();
            for (Map.Entry<String, Long> entry : cursors.entrySet()) {
                long incoming = entry.getValue() == null ? 0L : entry.getValue();
                Long existing = readCursors.get(entry.getKey());
                merged.put(entry.getKey(), existing == null ? incoming : Math.max(existing, incoming));
            }

            readCursors.clear();
            readCursors.putAll(merged);
            recomputeUnreadCounts();
        }
    }

    /** 한 사람의 커서만 올린다. 소켓 READ 프레임이 도착했을 때 쓴다. */
    void onReadCursorAdvanced(String roomId, String userId, long lastReadChatId) {
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;
            if (userId == null) return;

            Long current = readCursors.get(userId);
            // 커서 맵에 없는 사용자는 새로 추가하지 않는다. 빈 맵은 "커서를 아직 못 받아왔다"는
            // 뜻이지 "멤버가 한 명"이라는 뜻이 아니다 — 여기서 새 키를 만들면 분모가 줄어들어
            // 다른 멤버 몫까지 읽은 것으로 잘못 계산된다. 중간에 초대된 멤버도 마찬가지로
            // 다음 openRoom 의 GET 응답으로만 반영된다.
            if (current == null) return;

            // 커서는 단조 증가한다. 늦게 도착한 옛 이벤트가 되돌리지 못하게 한다.
            if (current >= lastReadChatId) return;

            readCursors.put(userId, lastReadChatId);
            recomputeUnreadCounts();
        }
    }

    /**
     * 활성 방 말풍선 전체의 숫자를 다시 계산한다.
     * 커서 하나만 바뀌어도 화면의 모든 말풍선이 영향을 받으므로 전체를 훑는다.
     * 반드시 activeRoomIndex 락 안에서 호출한다.
     */
    private void recomputeUnreadCounts() {
        if (readCursors.isEmpty()) return;

        Map<String, Long> effective = withImpliedCursors(readCursors, activeRoomIndex, myUserId);
        for (Map.Entry<Long, ChatBubble> entry : activeRoomIndex.entrySet()) {
            ChatBubble bubble = entry.getValue();
            bubble.setUnreadCount(unreadCountFor(entry.getKey(), bubble.getSender(), effective));
        }
        activeRoomChats.postValue(new ArrayList<>(activeRoomIndex.values()));
    }

    void mergeChats(String roomId, List<ChatDto> chats, boolean prepend) {
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;

            if (prepend) {
                LinkedHashMap<Long, ChatBubble> merged = new LinkedHashMap<>();
                for (ChatDto dto : chats) {
                    if (dto.getId() == null) continue;
                    merged.put(dto.getId(), new ChatBubble(dto));
                }
                // 기존 항목이 나중에 들어가므로 중복 시 기존 값이 살아남고 순서는 앞이 유지된다
                merged.putAll(activeRoomIndex);
                activeRoomIndex.clear();
                activeRoomIndex.putAll(merged);
            } else {
                for (ChatDto dto : chats) {
                    if (dto.getId() == null) continue;
                    activeRoomIndex.put(dto.getId(), new ChatBubble(dto));
                }
            }
            recomputeUnreadCounts();
            activeRoomChats.postValue(new ArrayList<>(activeRoomIndex.values()));
        }
    }

    // ---------- ChatSocketListener ----------

    @Override
    public void onChatReceived(ChatDto dto) {
        handleChat(dto, false);
    }

    @Override
    public void onReadReceived(String roomId, String userId, long lastReadChatId) {
        onReadCursorAdvanced(roomId, userId, lastReadChatId);
    }

    @Override
    public void onStateChanged(ConnectionState state) {
        connectionState.postValue(state);
    }

    @Override
    public void onReconnected() {
        // 끊겨 있던 동안의 공백을 메꾼다. 배너는 띄우지 않는다.
        flushPendingReadReceipts();
        refreshChatRooms();

        String roomId = activeRoomId;
        if (roomId == null) return;

        APIHelper.enqueueWithRetry(
                chatApi.RequestChatListSize(roomId, String.valueOf(GAP_RECOVERY_SIZE)),
                new Callback<List<ChatDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ChatDto>> call,
                                           @NonNull Response<List<ChatDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        for (ChatDto dto : response.body()) {
                            handleChat(dto, true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ChatDto>> call, @NonNull Throwable t) {
                        Log.e(TAG, "gap recovery failed", t);
                    }
                });
    }

    @Override
    public void onAuthFailure() {
        Log.w(TAG, "socket handshake unauthorized");
        // 기존 재발급 경로(RetryAbleCallback)를 태워 새 토큰을 DataStore 에 저장시킨다.
        // Credentials.accessToken() 은 매 연결마다 DataStore 를 읽으므로
        // 예약된 재연결이 자동으로 새 토큰을 사용한다.
        refreshChatRooms();
    }

    // ---------- 라우팅 ----------

    /** 패키지 가시성. 테스트가 소켓 없이 라우팅 규칙을 검증한다. */
    void handleChat(ChatDto dto, boolean isGapRecovery) {
        if (dto == null || dto.getRoomId() == null) return;

        if (dto.getId() == null) {
            Log.w(TAG, "chat without id dropped, room=" + dto.getRoomId());
            return;
        }

        if (myUserId.isEmpty()) {
            // 로그아웃 이후 도착한 잔여 메시지. disconnect() 는 비동기라 이미 wire 에 실린
            // 프레임까지 막지 못하므로 여기서 버린다. 방 목록/말풍선/배너 어디에도 반영하지 않는다.
            Log.d(TAG, "chat dropped, no identity (logged out)");
            return;
        }

        updateRoomOrder(dto);

        // activeRoomId 판정과 말풍선 추가는 같은 락 안에서 원자적으로 이뤄져야 한다.
        // handleChat 은 OkHttp 콜백 스레드에서, openRoom/closeRoom 은 메인 스레드에서
        // 실행되므로, 락 밖에서 판정하면 방을 전환하는 순간 이전 방의 메시지가 새 방에 섞인다.
        boolean isActiveRoom;
        synchronized (activeRoomIndex) {
            isActiveRoom = dto.getRoomId().equals(activeRoomId);
            if (isActiveRoom) {
                activeRoomIndex.put(dto.getId(), new ChatBubble(dto));
                recomputeUnreadCounts();
                activeRoomChats.postValue(new ArrayList<>(activeRoomIndex.values()));
            }
        }

        // 보고 있는 방에 온 메시지는 곧바로 읽은 것이다. 갭 복구분은 제외한다 —
        // 재연결 직후 밀린 메시지마다 READ 를 쏘면 프레임만 늘고 결과는 같다.
        if (isActiveRoom && !isGapRecovery) {
            sendReadReceipt(dto.getRoomId());
        }

        boolean mine = myUserId.equals(dto.getSender());
        if (!mine && !isActiveRoom && !isGapRecovery) {
            incrementUnread(dto.getRoomId());
            banner.postValue(new BannerEvent(
                    dto.getRoomId(), dto.getSender(), dto.getMessage(), dto.getChatType()));
        }
    }

    private void updateRoomOrder(ChatDto dto) {
        synchronized (roomCache) {
            for (int i = 0; i < roomCache.size(); i++) {
                ChatRoom room = roomCache.get(i);
                if (!dto.getRoomId().equals(room.getRoomId())) continue;

                room.setLastChatMsg(dto.getMessage());
                room.setLastChatId(String.valueOf(dto.getId()));
                room.setLastChatTime(dto.getChatTime());

                roomCache.remove(i);
                roomCache.add(0, room);
                chatRooms.postValue(new ArrayList<>(roomCache));
                return;
            }
        }
    }
}
