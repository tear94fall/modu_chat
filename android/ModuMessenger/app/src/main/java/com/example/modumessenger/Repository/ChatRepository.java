package com.example.modumessenger.Repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import com.example.modumessenger.entity.ChatRoom;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
    }

    public void closeRoom(String roomId) {
        synchronized (activeRoomIndex) {
            if (roomId == null || !roomId.equals(activeRoomId)) return;

            activeRoomId = null;
            activeRoomIndex.clear();
            activeRoomChats.postValue(new ArrayList<>());
        }
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
