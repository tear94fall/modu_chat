package com.example.modumessenger.Global;

import static com.example.modumessenger.Global.DataStoreHelper.checkDataStoreKey;
import static com.example.modumessenger.Global.DataStoreHelper.delDataStoreObject;
import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreStr;
import static com.example.modumessenger.Global.DataStoreHelper.initDataStore;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.modumessenger.BuildConfig;
import com.example.modumessenger.Global.socket.AndroidNetworkMonitor;
import com.example.modumessenger.Global.socket.MainThreadScheduler;
import com.example.modumessenger.Global.socket.OkHttpWebSocketManager;
import com.example.modumessenger.Global.socket.ReconnectPolicy;
import com.example.modumessenger.Global.socket.WebSocketManager;
import com.example.modumessenger.Repository.ChatRepository;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.entity.Member;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    private static final String WS_PATH = "ws-service/modu-chat";

    private static WebSocketManager webSocketManager;
    private static ChatRepository chatRepository;

    public static WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }

    public static ChatRepository getChatRepository() {
        return chatRepository;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initDataStore(getApplicationContext(), "modu-chat");

        webSocketManager = new OkHttpWebSocketManager(
                BuildConfig.WS_BASE_URL + WS_PATH,
                new OkHttpWebSocketManager.Credentials() {
                    @Override
                    public String userId() {
                        // DataStoreHelper.getDataStoreMember() 는 저장된 member 가 없으면
                        // null.toString() 으로 NPE 를 던진다(DataStoreHelper.java:47).
                        // 소켓 연결은 로그인 직후 member 저장 전 구간에서도 시도될 수 있다.
                        if (!Boolean.TRUE.equals(checkDataStoreKey("member"))) return "";

                        Member member = getDataStoreMember();
                        return member == null || member.getUserId() == null ? "" : member.getUserId();
                    }

                    @Override
                    public String accessToken() {
                        String token = getDataStoreStr("access-token");
                        return token == null ? "" : token;
                    }
                },
                new ReconnectPolicy(),
                new MainThreadScheduler(),
                new AndroidNetworkMonitor(getApplicationContext()));

        chatRepository = new ChatRepository(
                webSocketManager,
                RetrofitClient.createChatApiService(),
                RetrofitClient.createChatRoomApiService());

        ProcessLifecycleOwner.get().getLifecycle()
                .addObserver(new AppLifecycleObserver(webSocketManager));

        seedIdentity();
    }

    /**
     * DataStore 의 신원을 Repository 에 넣는다. 저장된 member 가 없으면 아무것도 하지 않는다.
     * onCreate 에서도 호출하는 이유: FCM 알림을 탭해 ChatActivity 로 바로 진입하면
     * LoginActivity 가 실행되지 않아 onLoggedIn() 이 돌지 않는다. 신원이 비어 있으면
     * ChatRepository.handleChat 이 모든 수신 메시지를 버린다.
     */
    public static void seedIdentity() {
        if (chatRepository == null) return;
        if (!Boolean.TRUE.equals(checkDataStoreKey("member"))) return;

        Member member = getDataStoreMember();
        if (member == null) return;

        chatRepository.setIdentity(member.getUserId(), String.valueOf(member.getId()));
    }

    /** 로그인 성공 직후 호출한다. 신원을 세팅하고 즉시 연결한다. */
    public static void onLoggedIn() {
        seedIdentity();
        webSocketManager.connect();
    }

    /** 로그아웃 시 호출한다. */
    public static void onLoggedOut() {
        // 방 목록을 비우기 전에 토픽을 해제해야 한다(roomId 가 필요하다).
        // 해제하지 않으면 다음 계정이 이전 계정 방의 푸시를 계속 받는다.
        List<ChatRoom> rooms = chatRepository.getChatRooms().getValue();
        if (rooms != null) {
            for (ChatRoom room : rooms) {
                if (room.getRoomId() != null) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(room.getRoomId());
                }
            }
        }

        // 신원을 먼저 비운다. handleChat 이 이를 보고 잔여 메시지를 전부 버리므로,
        // 아래의 정리 작업이 뒤늦은 메시지에 의해 되돌려지지 않는다.
        chatRepository.setIdentity("", null);
        webSocketManager.disconnect();

        // ChatRepository 는 프로세스 수명 동안 살아있는 싱글턴이다. 신원만 비우면
        // 다음 계정이 이전 계정의 방 목록과 활성 방을 그대로 보게 된다.
        String activeRoom = chatRepository.getActiveRoomId();
        if (activeRoom != null) {
            chatRepository.closeRoom(activeRoom);
        }
        chatRepository.setChatRooms(new ArrayList<>());
        chatRepository.clearBanner();

        // clearDataStore() 는 실제로 값을 지우지 못한다: DataStore 파일이
        // filesDir/datastore/ 안에 있어 비어있지 않은 디렉터리의 File.delete() 가 실패하고,
        // 지워지더라도 살아있는 RxDataStore 의 인메모리 캐시가 값을 계속 내준다.
        // 명시적으로 지우지 않으면 로그아웃 후 포그라운드 복귀 시 seedIdentity() 가
        // 이전 계정 신원을 되살리고, 이어지는 refreshChatRooms 가 이전 계정 방 목록을 다시 채운다.
        delDataStoreObject("member");
        delDataStoreObject("access-token");
        delDataStoreObject("refresh-token");
    }
}
