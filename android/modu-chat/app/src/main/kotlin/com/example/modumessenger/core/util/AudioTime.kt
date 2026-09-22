package com.example.modumessenger.core.util

import java.util.Locale

/** 음성 재생 시각 문구. `m:ss`, 한 시간을 넘으면 `h:mm:ss`. 음수는 0 으로 본다. */
object AudioTime {
    fun of(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) + 500L) / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }
}
