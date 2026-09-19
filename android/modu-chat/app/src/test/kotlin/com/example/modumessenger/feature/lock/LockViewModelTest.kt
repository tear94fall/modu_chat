package com.example.modumessenger.feature.lock

import app.cash.turbine.test
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.lock.FakeLockStorage
import com.example.modumessenger.core.lock.MutableClock
import com.example.modumessenger.core.lock.PinHasher
import com.example.modumessenger.core.session.SessionLogout
import com.example.modumessenger.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val storage = FakeLockStorage()
    private val clock = MutableClock()
    private val sessionLogout: SessionLogout = mockk(relaxed = true)

    private fun appLock(): AppLock =
        AppLock(storage, PinHasher(iterations = 100), clock, CoroutineScope(mainDispatcherRule.dispatcher))

    private suspend fun lockedViewModel(): Pair<AppLock, LockViewModel> {
        val lock = appLock()
        lock.setPin("1234")
        lock.onForeground()
        return lock to LockViewModel(lock, sessionLogout)
    }

    /** 4자리를 치고 검증 딜레이만큼만 시간을 민다(카운트다운은 건드리지 않는다). */
    private fun TestScope.type(vm: LockViewModel, pin: String) {
        pin.forEach { vm.onDigit(it) }
        advanceTimeBy(LockViewModel.SUBMIT_DELAY_MS + 1)
        runCurrent()
    }

    @Test
    fun `숫자를 누적하고 지운다`() = runTest {
        val (_, vm) = lockedViewModel()
        vm.onDigit('1'); vm.onDigit('2'); vm.onDelete(); vm.onDigit('3')
        assertEquals("13", vm.uiState.value.pin)
    }

    @Test
    fun `4자리가 차면 짧은 딜레이 뒤 검증하고 맞으면 잠금이 풀린다`() = runTest {
        val (lock, vm) = lockedViewModel()
        "1234".forEach { vm.onDigit(it) }
        assertEquals("딜레이 동안은 4번째 점이 채워진 채로 보인다", "1234", vm.uiState.value.pin)
        assertTrue(lock.isLocked.value)
        advanceTimeBy(LockViewModel.SUBMIT_DELAY_MS + 1)
        runCurrent()
        assertFalse(lock.isLocked.value)
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun `틀리면 입력을 비우고 남은 횟수와 흔들림을 알린다`() = runTest {
        val (lock, vm) = lockedViewModel()
        type(vm, "0000")
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(lock.isLocked.value)
        assertEquals("", s.pin)
        assertEquals(4, s.attemptsLeft)
        assertEquals(1, s.shakeKey)
        // 다음 숫자를 누르면 오류 표시는 사라진다
        vm.onDigit('1')
        assertNull(vm.uiState.value.attemptsLeft)
    }

    @Test
    fun `5회 틀리면 30초 카운트다운 동안 입력을 막는다`() = runTest {
        val (_, vm) = lockedViewModel()
        repeat(5) { type(vm, "0000") }
        assertEquals(30, vm.uiState.value.lockedOutSeconds)
        assertFalse(vm.uiState.value.inputEnabled)
        vm.onDigit('1')
        assertEquals("", vm.uiState.value.pin)

        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals(20, vm.uiState.value.lockedOutSeconds)
        advanceTimeBy(21_000L)
        runCurrent()
        assertEquals(0, vm.uiState.value.lockedOutSeconds)
        assertTrue(vm.uiState.value.inputEnabled)
    }

    @Test
    fun `저장된 대기가 있으면 화면이 뜰 때부터 센다`() = runTest {
        val lock = appLock()
        lock.setPin("1234")
        repeat(5) { lock.verifyPin("0000") }
        clock.advance(12_000L)

        val vm = LockViewModel(lock, sessionLogout)
        assertEquals(18, vm.uiState.value.lockedOutSeconds)
    }

    @Test
    fun `생체 성공은 PIN 없이 잠금을 푼다`() = runTest {
        val (lock, vm) = lockedViewModel()
        vm.onBiometricSuccess()
        assertFalse(lock.isLocked.value)
    }

    @Test
    fun `잊음은 로그아웃 시퀀스를 돌리고 알린다`() = runTest {
        val (_, vm) = lockedViewModel()
        coEvery { sessionLogout.run() } returns Unit
        vm.loggedOut.test {
            vm.forgot()
            awaitItem()
            coVerify(exactly = 1) { sessionLogout.run() }
            assertFalse(vm.uiState.value.working)
        }
    }

    @Test
    fun `설정의 생체 사용 여부를 비춘다`() = runTest {
        val (lock, vm) = lockedViewModel()
        assertFalse(vm.uiState.value.biometricEnabled)
        lock.setBiometric(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.biometricEnabled)
    }
}
