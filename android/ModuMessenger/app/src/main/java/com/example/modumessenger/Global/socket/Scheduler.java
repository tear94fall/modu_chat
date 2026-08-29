package com.example.modumessenger.Global.socket;

/** 재연결 지연 실행. Handler 를 직접 쓰지 않는 이유는 JVM 단위 테스트를 위해서다. */
public interface Scheduler {

    void postDelayed(Runnable task, long delayMs);

    void cancel();
}
