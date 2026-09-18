package com.example.modumessenger.core.lock

/** 잠금 유예 시간. 앱이 뒤로 간 뒤 이 시간이 지나 돌아오면 잠근다. */
enum class LockGrace(val millis: Long) {
    IMMEDIATE(0L),
    SECONDS_30(30_000L),
    MINUTE_1(60_000L),
    MINUTES_5(300_000L),
    ;

    companion object {
        fun fromMillis(millis: Long): LockGrace = entries.firstOrNull { it.millis == millis } ?: IMMEDIATE
    }
}

/** 화면에 보여 주는 잠금 설정. PIN 해시 같은 비밀은 여기 없다. */
data class LockSettings(
    val enabled: Boolean = false,
    val grace: LockGrace = LockGrace.IMMEDIATE,
    val biometricEnabled: Boolean = false,
)

/** PIN 검증 결과. */
sealed interface PinResult {
    data object Ok : PinResult

    /** 틀렸다. [remaining] 은 대기에 걸리기까지 남은 시도 횟수. */
    data class Wrong(val remaining: Int) : PinResult

    /** 연속 실패로 대기 중. [secondsLeft] 초 뒤에 다시 시도할 수 있다. */
    data class LockedOut(val secondsLeft: Int) : PinResult
}
