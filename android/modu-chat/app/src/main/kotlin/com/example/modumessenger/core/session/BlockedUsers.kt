package com.example.modumessenger.core.session

import com.example.modumessenger.core.di.ApplicationScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내가 차단한 친구들의 userId. 차단은 서버 메시지 흐름을 바꾸지 않고 **앱에서 거르므로**
 * 말풍선·배너·알림이 모두 이 값을 본다.
 *
 * DataStore `blocked-ids` 에 JSON 배열로 남겨 두어 FCM 이 콜드 스타트로 깨워도 알림을 억제할 수 있다.
 */
@Singleton
class BlockedUsers @Inject constructor(
    private val store: SessionStore,
    private val gson: Gson,
    @ApplicationScope scope: CoroutineScope,
) {

    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    private val mutex = Mutex()

    init {
        scope.launch { restore() }
    }

    fun isBlocked(userId: String): Boolean = userId.isNotBlank() && userId in _ids.value

    /** 서버가 알려 준 목록으로 통째로 갈아 끼운다. */
    suspend fun replaceAll(ids: Set<String>) {
        mutex.withLock {
            _ids.value = ids.toSet()
            persist()
        }
    }

    /** 차단/해제 한 명만 반영한다(목록을 다시 받지 않아도 화면이 바로 바뀐다). */
    suspend fun set(userId: String, blocked: Boolean) {
        if (userId.isBlank()) return
        mutex.withLock {
            val next = _ids.value.toMutableSet()
            if (blocked) next += userId else next -= userId
            if (next == _ids.value) return@withLock
            _ids.value = next
            persist()
        }
    }

    private suspend fun restore() {
        val json = store.blockedIdsJson() ?: return
        val restored = runCatching {
            gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
        }.getOrNull() ?: return
        mutex.withLock {
            // 복원 중에 서버 응답이 먼저 들어왔으면 그것이 더 새것이다.
            if (_ids.value.isEmpty()) _ids.value = restored.filter { it.isNotBlank() }.toSet()
        }
    }

    private suspend fun persist() {
        store.saveBlockedIdsJson(gson.toJson(_ids.value.toList()))
    }
}
