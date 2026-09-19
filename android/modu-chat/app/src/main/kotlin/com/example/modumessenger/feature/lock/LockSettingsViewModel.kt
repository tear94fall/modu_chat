package com.example.modumessenger.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modumessenger.R
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.lock.LockGrace
import com.example.modumessenger.core.lock.LockSettings
import com.example.modumessenger.core.lock.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** PIN 입력이 필요한 단계. NONE 이면 설정 목록을 보여 준다. */
enum class PinStep { NONE, ENABLE_NEW, ENABLE_CONFIRM, DISABLE_VERIFY, CHANGE_CURRENT, CHANGE_NEW, CHANGE_CONFIRM }

data class LockSettingsUiState(
    val settings: LockSettings = LockSettings(),
    val step: PinStep = PinStep.NONE,
    val pin: String = "",
    /** 키패드 위에 붙는 안내(불일치 등). null 이면 단계 기본 문구. */
    val hintRes: Int? = null,
    val attemptsLeft: Int? = null,
    val lockedOutSeconds: Int = 0,
    val shakeKey: Int = 0,
) {
    val inputEnabled: Boolean get() = lockedOutSeconds == 0
    val isError: Boolean get() = hintRes != null || attemptsLeft != null || lockedOutSeconds > 0
}

/**
 * 잠금 설정. 켜기(입력→재입력), 끄기(PIN 확인), PIN 변경(현재→새→재입력)을 단계 상태기계로 돌린다.
 * 재입력이 다르면 처음부터 다시 받는다.
 */
@HiltViewModel
class LockSettingsViewModel @Inject constructor(
    private val appLock: AppLock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockSettingsUiState())
    val uiState: StateFlow<LockSettingsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** 스낵바 문구. */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    /** 첫 입력(재입력 비교용)과 PIN 변경의 현재 PIN. 단계가 끝나면 비운다. */
    private var firstPin: String? = null
    private var currentPin: String? = null
    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            appLock.settings.collect { s -> _uiState.update { it.copy(settings = s) } }
        }
    }

    fun startEnable() = enter(PinStep.ENABLE_NEW)

    fun startDisable() = enter(PinStep.DISABLE_VERIFY)

    fun startChangePin() = enter(PinStep.CHANGE_CURRENT)

    fun cancelStep() {
        firstPin = null
        currentPin = null
        _uiState.update { it.copy(step = PinStep.NONE, pin = "", hintRes = null, attemptsLeft = null) }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.step == PinStep.NONE || !state.inputEnabled || !digit.isDigit() || state.pin.length >= AppLock.PIN_LENGTH) return
        val next = state.pin + digit
        _uiState.update { it.copy(pin = next, hintRes = null, attemptsLeft = null) }
        if (next.length == AppLock.PIN_LENGTH) submit(next)
    }

    fun onDelete() {
        if (!_uiState.value.inputEnabled) return
        _uiState.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    /** [available] 은 화면이 BiometricManager 로 알아낸 기기 등록 여부. 없으면 켜지 못하게 안내만 한다. */
    fun setBiometric(enabled: Boolean, available: Boolean) {
        if (enabled && !available) {
            _messages.tryEmit(R.string.lock_settings_biometric_unavailable)
            return
        }
        viewModelScope.launch { appLock.setBiometric(enabled) }
    }

    fun setGrace(grace: LockGrace) {
        viewModelScope.launch { appLock.setGrace(grace) }
    }

    private fun enter(step: PinStep) {
        firstPin = null
        currentPin = null
        _uiState.update { it.copy(step = step, pin = "", hintRes = null, attemptsLeft = null) }
    }

    private fun submit(pin: String) {
        viewModelScope.launch {
            // 4번째 점이 채워지는 애니메이션이 보인 뒤에 결과를 낸다. 곧바로 화면이 바뀌면 입력이 씹힌 것처럼 보인다.
            delay(SUBMIT_DELAY_MS)
            when (_uiState.value.step) {
                PinStep.ENABLE_NEW -> {
                    firstPin = pin
                    _uiState.update { it.copy(step = PinStep.ENABLE_CONFIRM, pin = "") }
                }
                PinStep.ENABLE_CONFIRM -> {
                    if (pin == firstPin) {
                        appLock.setPin(pin)
                        finish(R.string.lock_settings_enabled_done)
                    } else {
                        mismatch(PinStep.ENABLE_NEW)
                    }
                }
                PinStep.DISABLE_VERIFY -> onPinResult(appLock.disable(pin)) { finish(R.string.lock_settings_disabled_done) }
                PinStep.CHANGE_CURRENT -> onPinResult(appLock.checkPin(pin)) {
                    currentPin = pin
                    _uiState.update { it.copy(step = PinStep.CHANGE_NEW, pin = "") }
                }
                PinStep.CHANGE_NEW -> {
                    firstPin = pin
                    _uiState.update { it.copy(step = PinStep.CHANGE_CONFIRM, pin = "") }
                }
                PinStep.CHANGE_CONFIRM -> {
                    val current = currentPin
                    if (pin == firstPin && current != null) {
                        onPinResult(appLock.changePin(current, pin)) { finish(R.string.lock_settings_pin_changed) }
                    } else {
                        mismatch(PinStep.CHANGE_NEW)
                    }
                }
                PinStep.NONE -> Unit
            }
        }
    }

    private inline fun onPinResult(result: PinResult, onOk: () -> Unit) {
        when (result) {
            PinResult.Ok -> onOk()
            is PinResult.Wrong -> _uiState.update { it.copy(pin = "", attemptsLeft = result.remaining, shakeKey = it.shakeKey + 1) }
            is PinResult.LockedOut -> {
                _uiState.update { it.copy(pin = "", attemptsLeft = null, shakeKey = it.shakeKey + 1) }
                startCountdown(result.secondsLeft)
            }
        }
    }

    private fun mismatch(restartAt: PinStep) {
        firstPin = null
        _uiState.update { it.copy(step = restartAt, pin = "", hintRes = R.string.lock_setup_mismatch, shakeKey = it.shakeKey + 1) }
    }

    private fun finish(messageRes: Int) {
        firstPin = null
        currentPin = null
        _uiState.update { it.copy(step = PinStep.NONE, pin = "", hintRes = null, attemptsLeft = null) }
        _messages.tryEmit(messageRes)
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                _uiState.update { it.copy(lockedOutSeconds = left) }
                delay(1_000L)
                left--
            }
            _uiState.update { it.copy(lockedOutSeconds = 0) }
        }
    }

    companion object {
        const val SUBMIT_DELAY_MS = 220L
    }
}
