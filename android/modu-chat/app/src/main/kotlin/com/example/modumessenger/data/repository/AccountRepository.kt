package com.example.modumessenger.data.repository

import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.data.api.MemberApi
import javax.inject.Inject

/** 계정 자체에 대한 작업. 지금은 회원 탈퇴 하나다. */
interface AccountRepository {
    /**
     * 회원 탈퇴. 서버가 방 나가기·푸시 토큰·친구 관계를 정리하고 회원 행을 탈퇴 상태로 바꾼다.
     * 실패하면 [Result.failure] — 로컬 세션은 손대지 않으므로 화면이 다시 시도할 수 있다.
     */
    suspend fun withdraw(): Result<Unit>
}

class AccountRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi,
    private val sessionStore: SessionStore,
) : AccountRepository {

    override suspend fun withdraw(): Result<Unit> {
        val me = sessionStore.memberNow() ?: return Result.failure(IllegalStateException("로그인 상태가 아니다"))
        return runCatching { memberApi.withdraw(me.userId) }
    }
}
