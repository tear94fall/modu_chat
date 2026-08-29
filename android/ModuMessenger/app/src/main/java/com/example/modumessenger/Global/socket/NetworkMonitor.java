package com.example.modumessenger.Global.socket;

public interface NetworkMonitor {

    void start(Runnable onNetworkAvailable);

    void stop();
}
