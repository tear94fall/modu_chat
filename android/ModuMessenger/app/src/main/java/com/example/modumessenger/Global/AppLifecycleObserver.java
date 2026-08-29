package com.example.modumessenger.Global;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.modumessenger.Global.socket.WebSocketManager;

/**
 * 프로세스 가시성에 맞춰 소켓을 붙였다 뗀다.
 * Activity 단위가 아니라 프로세스 단위라 화면 회전과 화면 간 이동에는 반응하지 않는다.
 * (ProcessLifecycleOwner 내부 700ms 디바운스)
 */
public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private static final String TAG = "AppLifecycleObserver";

    private static volatile boolean foreground = false;

    private final WebSocketManager webSocketManager;

    public AppLifecycleObserver(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    /** FCM 알림 억제 판정에 쓰인다. */
    public static boolean isForeground() {
        return foreground;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        foreground = true;

        // 토큰만 있고 member 가 아직 없는 구간(로그인 진행 중)에는 연결하지 않는다.
        // 그 상태로 붙어도 서버가 userId 헤더 부재로 세션을 거절한다.
        if (Boolean.TRUE.equals(DataStoreHelper.checkDataStoreKey("access-token"))
                && Boolean.TRUE.equals(DataStoreHelper.checkDataStoreKey("member"))) {
            Log.d(TAG, "app foreground, connecting socket");
            App.seedIdentity();
            webSocketManager.connect();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        foreground = false;
        Log.d(TAG, "app background, disconnecting socket");
        webSocketManager.disconnect();
    }
}
