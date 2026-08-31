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
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.ChatRoomUnreadDto;
import com.example.modumessenger.entity.ChatRoom;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            activeRoomChats.postValue(new ArrayList<>());
        }
        // 서버 반영은 closeRoom 에서 한다. 여기서는 체감을 위해 로컬만 0 으로.
        clearUnread(roomId);
    }

    public void closeRoom(String roomId) {
        String memberId = myMemberId;

        // clearUnread 와 서버 호출은 이 블록 밖에 둔다. clearUnread 가 roomCache 락을 잡으므로
        // 안쪽에 두면 handleChat(roomCache -> activeRoomIndex)과 반대 순서로 두 락이 겹친다.
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;

            activeRoomId = null;
            activeRoomIndex.clear();
            activeRoomChats.postValue(new ArrayList<>());
        }

        clearUnread(roomId);

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
            activeRoomChats.postValue(new ArrayList<>(activeRoomIndex.values()));
        }
    }

    // ---------- ChatSocketListener ----------

    @Override
    public void onChatReceived(ChatDto dto) {
        handleChat(dto, false);
    }

    @Override
    public void onStateChanged(ConnectionState state) {
        connectionState.postValue(state);
    }

    @Override
    public void onReconnected() {
        // 끊겨 있던 동안의 공백을 메꾼다. 배너는 띄우지 않는다.
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
                activeRoomChats.postValue(new ArrayList<>(activeRoomIndex.values()));
            }
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
