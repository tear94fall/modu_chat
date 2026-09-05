package com.example.modumessenger.Global.socket;

import java.util.Random;

/**
 * 재연결 지연 계산기. 1s 부터 2배씩 증가하며 30s 에서 상한에 걸린다.
 * 서버 재시작 시 전 클라이언트가 동시에 몰리는 것을 막기 위해 ±20% jitter 를 적용한다.
 */
public class ReconnectPolicy {

    public static final long INITIAL_DELAY_MS = 1000L;
    public static final long MAX_DELAY_MS = 30000L;

    private static final double JITTER_RATIO = 0.2d;

    private final Random random;
    private int attempt = 0;

    public ReconnectPolicy() {
        this(new Random());
    }

    public ReconnectPolicy(Random random) {
        this.random = random;
    }

    public long nextDelayMs() {
        long base = INITIAL_DELAY_MS;
        for (int i = 0; i < attempt && base < MAX_DELAY_MS; i++) {
            base = base * 2;
        }
        if (base > MAX_DELAY_MS) {
            base = MAX_DELAY_MS;
        }
        attempt++;

        double factor = (1.0d - JITTER_RATIO) + (2.0d * JITTER_RATIO * random.nextDouble());
        return Math.round(base * factor);
    }

    public void reset() {
        attempt = 0;
    }

    public int getAttempt() {
        return attempt;
    }
}
