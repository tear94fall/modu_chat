package com.example.modumessenger.core.lock

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.modumessenger.core.di.ApplicationScope
import com.example.modumessenger.core.session.SessionEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 잠금을 **프로세스** 생명주기에 묶는다(소켓의 `SocketLifecycle` 과 같은 자리).
 * 뒤로 가면 시각을 적고, 앞으로 오면 유예 시간을 따져 잠근다. 로그아웃이면 잠금을 통째로 지운다.
 */
@Singleton
class LockLifecycle @Inject constructor(
    private val appLock: AppLock,
    private val sessionEvents: SessionEvents,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private var started = false

    /** 메인 스레드에서 부른다(ProcessLifecycleOwner 요구). 두 번 불러도 안전하다. */
    fun start() {
        if (started) return
        started = true

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    scope.launch { appLock.onForeground() }
                }

                override fun onStop(owner: LifecycleOwner) {
                    appLock.onBackground()
                }
            },
        )

        scope.launch {
            sessionEvents.loggedOut.collect { appLock.clear() }
        }
    }
}
