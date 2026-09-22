package com.example.modumessenger.feature.lock

import app.cash.turbine.test
import com.example.modumessenger.R
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.lock.FakeLockStorage
import com.example.modumessenger.core.lock.LockGrace
import com.example.modumessenger.core.lock.MutableClock
import com.example.modumessenger.core.lock.PinHasher
import com.example.modumessenger.core.lock.PinResult
import com.example.modumessenger.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LockSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val storage = FakeLockStorage()
    private val clock = MutableClock()
    // 규칙이 Main 을 바꾼 뒤에 만들어야 하므로 lazy 로 둔다.
    private val appLock by lazy { AppLock(storage, PinHasher(iterations = 100), clock, CoroutineScope(mainDispatcherRule.dispatcher)) }
    private val vm by lazy { LockSettingsViewModel(appLock) }

    private fun TestScope.type(pin: String) {
        pin.forEach { vm.onDigit(it) }
        advanceTimeBy(LockSettingsViewModel.SUBMIT_DELAY_MS + 1)
        runCurrent()
    }

    @Test
    fun `켜기는 입력과 재입력이 같아야 PIN 을 저장한다`() = runTest {
        vm.messages.test {
            vm.startEnable()
            assertEquals(PinStep.ENABLE_NEW, vm.uiState.value.step)
            type("1234")
            assertEquals(PinStep.ENABLE_CONFIRM, vm.uiState.value.step)
            assertEquals("", vm.uiState.value.pin)
            type("1234")
            assertEquals(R.string.lock_settings_enabled_done, awaitItem())
            assertEquals(PinStep.NONE, vm.uiState.value.step)
            assertTrue(vm.uiState.value.settings.enabled)
            assertEquals(PinResult.Ok, appLock.checkPin("1234"))
        }
    }

    @Test
    fun `재입력이 다르면 처음부터 다시 받는다`() = runTest {
        vm.startEnable()
        type("1234")
        type("9999")
        val s = vm.uiState.value
        assertEquals(PinStep.ENABLE_NEW, s.step)
        assertEquals(R.string.lock_setup_mismatch, s.hintRes)
        assertEquals(1, s.shakeKey)
        assertFalse(storage.state.value.enabled)
        // 다시 입력하면 안내가 사라진다
        vm.onDigit('1')
        assertNull(vm.uiState.value.hintRes)
    }

    @Test
    fun `끄기는 PIN 이 맞아야 한다`() = runTest {
        appLock.setPin("1234")
        vm.startDisable()
        type("0000")
        assertEquals(4, vm.uiState.value.attemptsLeft)
        assertEquals(PinStep.DISABLE_VERIFY, vm.uiState.value.step)
        assertTrue(storage.state.value.enabled)
        type("1234")
        assertEquals(PinStep.NONE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.settings.enabled)
    }

    @Test
    fun `PIN 변경은 현재 확인 후 새 PIN 을 두 번 받는다`() = runTest {
        appLock.setPin("1234")
        vm.startChangePin()
        type("1234")
        assertEquals(PinStep.CHANGE_NEW, vm.uiState.value.step)
        type("5678")
        assertEquals(PinStep.CHANGE_CONFIRM, vm.uiState.value.step)
        type("5678")
        assertEquals(PinStep.NONE, vm.uiState.value.step)
        assertEquals(PinResult.Ok, appLock.checkPin("5678"))
        assertTrue(appLock.checkPin("1234") is PinResult.Wrong)
    }

    @Test
    fun `취소하면 단계와 입력을 버린다`() = runTest {
        vm.startEnable()
        type("12")
        vm.cancelStep()
        assertEquals(PinStep.NONE, vm.uiState.value.step)
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun `생체가 등록되지 않은 기기에서는 켜지 못하고 안내만 한다`() = runTest {
        appLock.setPin("1234")
        vm.messages.test {
            vm.setBiometric(enabled = true, available = false)
            assertEquals(R.string.lock_settings_biometric_unavailable, awaitItem())
            assertFalse(vm.uiState.value.settings.biometricEnabled)
        }
        vm.setBiometric(enabled = true, available = true)
        assertTrue(vm.uiState.value.settings.biometricEnabled)
    }

    @Test
    fun `유예 시간을 저장한다`() = runTest {
        appLock.setPin("1234")
        vm.setGrace(LockGrace.MINUTE_1)
        assertEquals(LockGrace.MINUTE_1, vm.uiState.value.settings.grace)
    }
}
