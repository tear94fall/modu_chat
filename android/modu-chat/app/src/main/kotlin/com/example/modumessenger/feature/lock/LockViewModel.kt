package com.example.modumessenger.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.lock.PinResult
import com.example.modumessenger.core.session.SessionLogout
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

data class LockUiState(
    val pin: String = "",
    /** 방금 틀렸을 때 남은 시도 횟수. null 이면 오류 표시 없음. */
    val attemptsLeft: Int? = null,
    /** 0 이면 대기 없음. 1 이상이면 키패드를 막고 카운트다운을 보여 준다. */
    val lockedOutSeconds: Int = 0,
    /** 틀릴 때마다 올라간다. 화면이 흔들림 애니메이션의 트리거로 쓴다. */
    val shakeKey: Int = 0,
    val biometricEnabled: Boolean = false,
    /** 로그아웃 진행 중. 입력을 막는다. */
    val working: Boolean = false,
) {
    val inputEnabled: Boolean get() = lockedOutSeconds == 0 && !working
}

/** 잠금 화면. 4자리가 차면 바로 검증하고, 대기에 걸리면 초를 센다. */
@HiltViewModel
class LockViewModel @Inject constructor(
    private val appLock: AppLock,
    private val sessionLogout: SessionLogout,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** "PIN 을 잊으셨나요?" 로그아웃이 끝났다. 화면이 구글 signOut 을 하고 로그인으로 간다. */
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            appLock.settings.collect { s -> _uiState.update { it.copy(biometricEnabled = s.biometricEnabled) } }
        }
        viewModelScope.launch {
            val left = appLock.lockoutSecondsLeft()
            if (left > 0) startCountdown(left)
        }
    }

    /** 잠금 화면이 다시 보일 때 입력 찌꺼기를 비운다. 뷰모델은 액티비티 수명이라 잠금 사이에 살아남는다. */
    fun reset() {
        _uiState.update { it.copy(pin = "", attemptsLeft = null, working = false) }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (!state.inputEnabled || !digit.isDigit() || state.pin.length >= AppLock.PIN_LENGTH) return
        val next = state.pin + digit
        _uiState.update { it.copy(pin = next, attemptsLeft = null) }
        if (next.length == AppLock.PIN_LENGTH) submit(next)
    }

    fun onDelete() {
        if (!_uiState.value.inputEnabled) return
        _uiState.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    fun onBiometricSuccess() {
        appLock.unlock()
    }

    fun forgot() {
        if (_uiState.value.working) return
        _uiState.update { it.copy(working = true) }
        viewModelScope.launch {
            sessionLogout.run()
            _uiState.update { it.copy(working = false, pin = "") }
            _loggedOut.tryEmit(Unit)
        }
    }

    private fun submit(pin: String) {
        viewModelScope.launch {
            // 4번째 점이 채워지는 애니메이션이 보인 뒤에 결과를 낸다. 곧바로 화면이 바뀌면 입력이 씹힌 것처럼 보인다.
            delay(SUBMIT_DELAY_MS)
            when (val result = appLock.verifyPin(pin)) {
                PinResult.Ok -> _uiState.update { it.copy(pin = "", attemptsLeft = null) }
                is PinResult.Wrong -> _uiState.update {
                    it.copy(pin = "", attemptsLeft = result.remaining, shakeKey = it.shakeKey + 1)
                }
                is PinResult.LockedOut -> {
                    _uiState.update { it.copy(pin = "", attemptsLeft = null, shakeKey = it.shakeKey + 1) }
                    startCountdown(result.secondsLeft)
                }
            }
        }
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
