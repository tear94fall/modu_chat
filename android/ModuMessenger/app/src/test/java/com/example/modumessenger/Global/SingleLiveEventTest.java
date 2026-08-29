package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SingleLiveEventTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static class TestLifecycleOwner implements LifecycleOwner {
        private final LifecycleRegistry registry = new LifecycleRegistry(this);

        TestLifecycleOwner() {
            registry.setCurrentState(Lifecycle.State.RESUMED);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    @Test
    public void deliversValueToObserver() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        List<String> received = new ArrayList<>();

        event.observe(new TestLifecycleOwner(), received::add);
        event.setValue("first");

        assertEquals(Arrays.asList("first"), received);
    }

    @Test
    public void doesNotRedeliverPastValueToNewObserver() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();

        event.observe(new TestLifecycleOwner(), first::add);
        event.setValue("only-once");

        event.observe(new TestLifecycleOwner(), second::add);

        assertEquals(Arrays.asList("only-once"), first);
        assertTrue("재구독한 옵저버는 지나간 이벤트를 받으면 안 된다", second.isEmpty());
    }

    @Test
    public void deliversEachNewValue() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        List<String> received = new ArrayList<>();

        event.observe(new TestLifecycleOwner(), received::add);
        event.setValue("a");
        event.setValue("b");

        assertEquals(Arrays.asList("a", "b"), received);
    }

    @Test
    public void clearPendingDropsUndeliveredEvent() {
        SingleLiveEvent<String> event = new SingleLiveEvent<>();
        event.setValue("stale");

        event.clearPending();

        List<String> received = new ArrayList<>();
        event.observe(new TestLifecycleOwner(), received::add);

        assertTrue("clearPending 이후 새 옵저버는 이전 이벤트를 받으면 안 된다", received.isEmpty());
    }
}
