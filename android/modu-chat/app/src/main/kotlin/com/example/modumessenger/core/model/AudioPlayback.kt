package com.example.modumessenger.core.model

/**
 * 지금 재생기에 올라 있는 음성 메시지의 상태. 앱 전체에 재생기는 하나라 다른 음성을 누르면 이걸로 바뀐다.
 * [positionMs]/[durationMs] 로 말풍선이 현재 시각·남은 시각을 그린다.
 */
data class AudioPlayback(
    val fileName: String,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isMuted: Boolean = false,
    val failed: Boolean = false,
) {
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
    val progress: Float get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}
