package com.example.modumessenger.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modumessenger.core.session.SessionEvents
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.data.api.ChatRoomApi
import com.example.modumessenger.R
import com.example.modumessenger.data.repository.AccountRepository
import com.example.modumessenger.data.repository.AuthRepository
import com.example.modumessenger.data.repository.PushRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 계정 설정(부록 A §22). 로그아웃 순서가 중요하다:
 * 세션이 살아 있는 동안 방 목록을 받아 FCM 토픽을 끊고, 그다음에 세션을 지운다.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
    private val chatRoomApi: ChatRoomApi,
    private val sessionStore: SessionStore,
    private val sessionEvents: SessionEvents,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    /** 로그아웃이나 탈퇴가 진행 중이면 두 버튼 모두 잠근다. */
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** 스낵바로 띄울 문자열 리소스(탈퇴 실패 등). */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private val _loggedOut = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 화면이 받아 구글 `signOut` 을 하고 `login` 으로 간다. */
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    fun logout() {
        if (_isWorking.value) return
        _isWorking.value = true
        viewModelScope.launch {
            unsubscribeMyRooms()
            finishSession()
        }
    }

    /**
     * 회원 탈퇴. 서버 탈퇴가 성공한 뒤에만 로그아웃과 같은 정리를 한다.
     * 실패하면 세션은 그대로 두고 안내만 띄운다 — 다시 누르면 재시도된다(서버 쪽은 멱등).
     */
    fun withdraw() {
        if (_isWorking.value) return
        _isWorking.value = true
        viewModelScope.launch {
            // 토픽은 세션이 살아 있을 때 끊어야 하지만, 탈퇴가 실패하면 다시 구독해야 하므로 서버 응답 뒤에 끊는다.
            val result = accountRepository.withdraw()
            if (result.isFailure) {
                _isWorking.value = false
                _messages.tryEmit(R.string.account_withdraw_failed)
                return@launch
            }
            unsubscribeMyRooms()
            finishSession()
        }
    }

    /** 1) 세션이 지워지기 전에 방 토픽을 끊는다(방 id 가 필요하다). */
    private suspend fun unsubscribeMyRooms() {
        val me = sessionStore.memberNow() ?: return
        val roomIds = runCatching { chatRoomApi.getRooms(me.id.toString()) }
            .getOrNull()
            ?.mapNotNull { it.roomId }
            .orEmpty()
        if (roomIds.isNotEmpty()) pushRepository.unsubscribeRooms(roomIds)
    }

    /** 2) revoke 를 시도하고 세션을 지운 뒤 3) 앱 전체에 알린다(소켓 종료 등은 구독자가 한다). */
    private suspend fun finishSession() {
        authRepository.logout()
        sessionEvents.notifyLoggedOut()
        _isWorking.value = false
        _loggedOut.tryEmit(Unit)
    }
}
