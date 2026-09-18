package com.example.modumessenger.core.lock

import com.example.modumessenger.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 잠금의 단일 진실 원천. 설정·PIN 은 [LockStorage] 에, "지금 잠겨 있는가" 는 메모리에 둔다.
 *
 * - 프로세스가 새로 뜨면 잠금이 켜져 있는 한 잠긴 채 시작한다.
 * - 뒤로 갔다가 유예 시간이 지나 돌아오면 잠근다([onBackground]/[onForeground]).
 * - 5회 연속 틀리면 30초 대기. 횟수와 대기 시각은 저장되어 재시작으로 초기화되지 않는다.
 * - 로그아웃([clear])이면 전부 지운다. 다음 사람의 계정에 이전 PIN 이 남으면 안 된다.
 */
@Singleton
class AppLock @Inject constructor(
    private val storage: LockStorage,
    private val hasher: PinHasher,
    private val clock: Clock,
    @ApplicationScope scope: CoroutineScope,
) {

    val settings: Flow<LockSettings> = storage.data
        .map { LockSettings(it.enabled, LockGrace.fromMillis(it.graceMillis), it.biometricEnabled) }
        // 로컬 편의 기능이라 저장소 오류는 "잠금 꺼짐" 으로 본다.
        .catch { emit(LockSettings()) }

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /** 앱이 마지막으로 뒤로 간 시각. 프로세스 수명 동안만 의미가 있다. */
    @Volatile
    private var backgroundedAt: Long? = null

    init {
        scope.launch { if (data().enabled) _isLocked.value = true }
    }

    fun onBackground() {
        backgroundedAt = clock.millis()
    }

    suspend fun onForeground() {
        val data = data()
        if (!data.enabled) return
        val since = backgroundedAt
        if (since == null || clock.millis() - since >= data.graceMillis) _isLocked.value = true
    }

    /** 저장된 대기가 남아 있으면 남은 초, 아니면 0. 잠금 화면이 뜰 때 카운트다운을 이어 그리는 데 쓴다. */
    suspend fun lockoutSecondsLeft(): Int {
        val until = data().lockedUntil
        val now = clock.millis()
        return if (until > now) secondsLeft(until, now) else 0
    }

    /** 생체 인식 성공처럼 PIN 없이 푸는 경로. */
    fun unlock() {
        _isLocked.value = false
    }

    suspend fun setPin(pin: String) {
        val salt = hasher.newSalt()
        val hash = hasher.hash(pin, salt)
        storage.update { it.copy(enabled = true, pinHash = hash, pinSalt = salt, failedCount = 0, lockedUntil = 0L) }
    }

    /** 잠금 화면의 검증. 맞으면 잠금이 풀린다. */
    suspend fun verifyPin(pin: String): PinResult {
        val result = check(pin)
        if (result is PinResult.Ok) _isLocked.value = false
        return result
    }

    /** 잠금을 풀지 않고 PIN 만 확인한다(설정 화면의 "현재 PIN"). 실패 횟수 규칙은 같다. */
    suspend fun checkPin(pin: String): PinResult = check(pin)

    suspend fun changePin(current: String, new: String): PinResult {
        val result = check(current)
        if (result is PinResult.Ok) setPin(new)
        return result
    }

    suspend fun disable(pin: String): PinResult {
        val result = check(pin)
        if (result is PinResult.Ok) {
            storage.update { LockData() }
            _isLocked.value = false
        }
        return result
    }

    suspend fun setBiometric(enabled: Boolean) {
        storage.update { it.copy(biometricEnabled = enabled) }
    }

    suspend fun setGrace(grace: LockGrace) {
        storage.update { it.copy(graceMillis = grace.millis) }
    }

    suspend fun clear() {
        storage.update { LockData() }
        backgroundedAt = null
        _isLocked.value = false
    }

    /** 대기 중이면 [PinResult.LockedOut], 틀리면 횟수를 올리고 5회째에 대기를 건다. 맞으면 횟수를 지운다. */
    private suspend fun check(pin: String): PinResult {
        val data = data()
        val now = clock.millis()
        if (data.lockedUntil > now) return PinResult.LockedOut(secondsLeft(data.lockedUntil, now))

        val hash = data.pinHash
        val salt = data.pinSalt
        val ok = hash != null && salt != null && hasher.matches(pin, salt, hash)
        if (ok) {
            if (data.failedCount != 0 || data.lockedUntil != 0L) {
                storage.update { it.copy(failedCount = 0, lockedUntil = 0L) }
            }
            return PinResult.Ok
        }

        val failed = data.failedCount + 1
        return if (failed >= MAX_ATTEMPTS) {
            val until = now + LOCKOUT_MILLIS
            storage.update { it.copy(failedCount = 0, lockedUntil = until) }
            PinResult.LockedOut(secondsLeft(until, now))
        } else {
            storage.update { it.copy(failedCount = failed) }
            PinResult.Wrong(MAX_ATTEMPTS - failed)
        }
    }

    private suspend fun data(): LockData = runCatching { storage.data.first() }.getOrDefault(LockData())

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L

        fun secondsLeft(until: Long, now: Long): Int = ((until - now + 999) / 1000).toInt().coerceAtLeast(1)
    }
}
