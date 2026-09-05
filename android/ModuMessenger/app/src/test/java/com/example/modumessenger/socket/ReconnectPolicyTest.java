package com.example.modumessenger.socket;

import static org.junit.Assert.assertEquals;

import com.example.modumessenger.Global.socket.ReconnectPolicy;

import org.junit.Test;

import java.util.Random;

public class ReconnectPolicyTest {

    /** nextDouble() 을 고정해 jitter 를 결정적으로 만든다. 0.5 이면 배수가 정확히 1.0 이다. */
    private static class FixedRandom extends Random {
        private final double value;

        FixedRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }

    @Test
    public void producesExponentialSequence() {
        ReconnectPolicy policy = new ReconnectPolicy(new FixedRandom(0.5d));

        assertEquals(1000L, policy.nextDelayMs());
        assertEquals(2000L, policy.nextDelayMs());
        assertEquals(4000L, policy.nextDelayMs());
        assertEquals(8000L, policy.nextDelayMs());
        assertEquals(16000L, policy.nextDelayMs());
    }

    @Test
    public void capsAtThirtySeconds() {
        ReconnectPolicy policy = new ReconnectPolicy(new FixedRandom(0.5d));

        for (int i = 0; i < 5; i++) {
            policy.nextDelayMs();   // 1s, 2s, 4s, 8s, 16s
        }

        assertEquals(30000L, policy.nextDelayMs());
        assertEquals(30000L, policy.nextDelayMs());
    }

    @Test
    public void jitterStaysWithinTwentyPercent() {
        assertEquals(800L, new ReconnectPolicy(new FixedRandom(0.0d)).nextDelayMs());
        assertEquals(1200L, new ReconnectPolicy(new FixedRandom(1.0d)).nextDelayMs());
    }

    @Test
    public void resetReturnsToInitialDelay() {
        ReconnectPolicy policy = new ReconnectPolicy(new FixedRandom(0.5d));

        policy.nextDelayMs();
        policy.nextDelayMs();
        policy.reset();

        assertEquals(1000L, policy.nextDelayMs());
    }
}
