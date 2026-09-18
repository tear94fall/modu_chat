package com.example.modumessenger.core.lock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockTest {

    private val storage = FakeLockStorage()
    private val clock = MutableClock()
    private val hasher = PinHasher(iterations = 100)

    private fun TestScope.appLock(): AppLock =
        AppLock(storage, hasher, clock, CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))

    private suspend fun TestScope.enabledLock(pin: String = "1234"): AppLock {
        val lock = appLock()
        lock.setPin(pin)
        return lock
    }

    // ---------- PIN ----------

    @Test
    fun `setPin 은 잠금을 켜고 평문을 남기지 않는다`() = runTest {
        val lock = enabledLock()
        val data = storage.state.value
        assertTrue(data.enabled)
        assertTrue(data.pinHash != null && data.pinHash != "1234")
        assertTrue(lock.settings.first().enabled)
    }

    @Test
    fun `맞는 PIN 은 Ok 이고 잠금이 풀린다`() = runTest {
        val lock = enabledLock()
        lock.onForeground()
        assertTrue(lock.isLocked.value)
        assertEquals(PinResult.Ok, lock.verifyPin("1234"))
        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `틀리면 남은 횟수를 알려 주고 5회째에 30초 대기에 걸린다`() = runTest {
        val lock = enabledLock()
        lock.onForeground()
        assertEquals(PinResult.Wrong(4), lock.verifyPin("0000"))
        assertEquals(PinResult.Wrong(3), lock.verifyPin("0000"))
        assertEquals(PinResult.Wrong(2), lock.verifyPin("0000"))
        assertEquals(PinResult.Wrong(1), lock.verifyPin("0000"))
        assertEquals(PinResult.LockedOut(30), lock.verifyPin("0000"))
        // 대기 중에는 맞는 PIN 도 안 받는다
        clock.advance(10_000L)
        assertEquals(PinResult.LockedOut(20), lock.verifyPin("1234"))
        assertTrue(lock.isLocked.value)
        // 30초가 지나면 다시 5회를 준다
        clock.advance(20_000L)
        assertEquals(PinResult.Wrong(4), lock.verifyPin("0000"))
        assertEquals(PinResult.Ok, lock.verifyPin("1234"))
        assertEquals(0, storage.state.value.failedCount)
    }

    @Test
    fun `대기는 저장되어 새 인스턴스에서도 이어진다`() = runTest {
        val lock = enabledLock()
        repeat(5) { lock.verifyPin("0000") }
        val fresh = appLock()
        assertTrue("잠금이 켜져 있으면 잠긴 채 시작한다", fresh.isLocked.value)
        assertEquals(PinResult.LockedOut(30), fresh.verifyPin("1234"))
    }

    @Test
    fun `changePin 은 현재 PIN 이 맞을 때만 바꾼다`() = runTest {
        val lock = enabledLock()
        assertEquals(PinResult.Wrong(4), lock.changePin("9999", "5678"))
        assertEquals(PinResult.Ok, lock.changePin("1234", "5678"))
        lock.onForeground()
        assertEquals(PinResult.Wrong(4), lock.verifyPin("1234"))
        assertEquals(PinResult.Ok, lock.verifyPin("5678"))
    }

    @Test
    fun `checkPin 은 맞아도 잠금을 풀지 않는다`() = runTest {
        val lock = enabledLock()
        lock.onForeground()
        assertEquals(PinResult.Ok, lock.checkPin("1234"))
        assertTrue(lock.isLocked.value)
        assertEquals(PinResult.Wrong(4), lock.checkPin("0000"))
    }

    @Test
    fun `disable 은 PIN 확인 뒤 전부 지운다`() = runTest {
        val lock = enabledLock()
        lock.setBiometric(true)
        assertEquals(PinResult.Wrong(4), lock.disable("0000"))
        assertTrue(storage.state.value.enabled)
        assertEquals(PinResult.Ok, lock.disable("1234"))
        assertEquals(LockData(), storage.state.value)
        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `clear 는 PIN 없이 전부 지우고 잠금을 푼다`() = runTest {
        val lock = enabledLock()
        lock.onForeground()
        lock.clear()
        assertEquals(LockData(), storage.state.value)
        assertFalse(lock.isLocked.value)
        assertNull(storage.state.value.pinHash)
    }

    // ---------- 생명주기 ----------

    @Test
    fun `잠금이 꺼져 있으면 앞으로 와도 잠기지 않는다`() = runTest {
        val lock = appLock()
        lock.onForeground()
        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `유예가 즉시면 뒤로 갔다 오는 즉시 잠긴다`() = runTest {
        val lock = enabledLock()
        lock.verifyPin("1234")
        lock.onBackground()
        lock.onForeground()
        assertTrue(lock.isLocked.value)
    }

    @Test
    fun `유예 30초 안에 돌아오면 안 잠기고 지나면 잠긴다`() = runTest {
        val lock = enabledLock()
        lock.setGrace(LockGrace.SECONDS_30)
        lock.verifyPin("1234")

        lock.onBackground()
        clock.advance(29_999L)
        lock.onForeground()
        assertFalse(lock.isLocked.value)

        lock.onBackground()
        clock.advance(30_000L)
        lock.onForeground()
        assertTrue(lock.isLocked.value)
    }

    @Test
    fun `설정은 저장된 값을 그대로 비춘다`() = runTest {
        val lock = enabledLock()
        lock.setGrace(LockGrace.MINUTES_5)
        lock.setBiometric(true)
        assertEquals(LockSettings(enabled = true, grace = LockGrace.MINUTES_5, biometricEnabled = true), lock.settings.first())
    }
}
