package com.example.modumessenger.Global.socket;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

/**
 * 네트워크 복구를 감지해 즉시 재연결시킨다.
 * 네트워크가 없는 동안 백오프 타이머를 도는 것은 배터리 낭비이고 복구도 느리다.
 * ACCESS_NETWORK_STATE 권한은 이미 매니페스트에 선언되어 있다.
 */
public class AndroidNetworkMonitor implements NetworkMonitor {

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback callback;

    public AndroidNetworkMonitor(Context context) {
        this.connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public synchronized void start(Runnable onNetworkAvailable) {
        stop();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                onNetworkAvailable.run();
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, callback);
    }

    @Override
    public synchronized void stop() {
        if (callback == null) return;

        try {
            connectivityManager.unregisterNetworkCallback(callback);
        } catch (IllegalArgumentException alreadyUnregistered) {
            // 이미 해제된 콜백. 무시한다.
        }
        callback = null;
    }
}
