package com.example.modumessenger.core.lock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FakeLockStorage(initial: LockData = LockData()) : LockStorage {
    val state = MutableStateFlow(initial)
    override val data: Flow<LockData> = state
    override suspend fun update(transform: (LockData) -> LockData) {
        state.value = transform(state.value)
    }
}

/** 테스트가 시각을 밀어 움직이는 시계. */
class MutableClock(private var now: Long = 1_000_000L) : Clock() {
    fun advance(millis: Long) {
        now += millis
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(now)
    override fun millis(): Long = now
}
