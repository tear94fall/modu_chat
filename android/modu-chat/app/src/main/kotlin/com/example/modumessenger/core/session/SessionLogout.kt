package com.example.modumessenger.core.session

import com.example.modumessenger.data.api.ChatRoomApi
import com.example.modumessenger.data.repository.AuthRepository
import com.example.modumessenger.data.repository.PushRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그아웃 한 벌. 계정 설정과 잠금 화면("PIN 을 잊으셨나요?")이 같이 쓴다. 순서가 중요하다:
 * 1) 세션이 살아 있는 동안 방 토픽을 끊고(방 id 가 필요하다) 2) revoke 를 시도하고 세션을 지운 뒤
 * 3) 앱 전체에 알린다(소켓 종료·잠금 초기화는 구독자가 한다).
 */
@Singleton
class SessionLogout @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
    private val chatRoomApi: ChatRoomApi,
    private val sessionStore: SessionStore,
    private val sessionEvents: SessionEvents,
) {

    suspend fun run() {
        unsubscribeMyRooms()
        authRepository.logout()
        sessionEvents.notifyLoggedOut()
    }

    private suspend fun unsubscribeMyRooms() {
        val me = sessionStore.memberNow() ?: return
        val roomIds = runCatching { chatRoomApi.getRooms(me.id.toString()) }
            .getOrNull()
            ?.mapNotNull { it.roomId }
            .orEmpty()
        if (roomIds.isNotEmpty()) pushRepository.unsubscribeRooms(roomIds)
    }
}
