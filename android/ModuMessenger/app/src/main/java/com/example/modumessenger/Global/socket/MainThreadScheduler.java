package com.example.modumessenger.Global.socket;

import android.os.Handler;
import android.os.Looper;

public class MainThreadScheduler implements Scheduler {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;

    @Override
    public synchronized void postDelayed(Runnable task, long delayMs) {
        cancel();
        pending = task;
        handler.postDelayed(task, delayMs);
    }

    @Override
    public synchronized void cancel() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }
}
