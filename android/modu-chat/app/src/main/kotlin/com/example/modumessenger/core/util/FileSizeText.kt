package com.example.modumessenger.core.util

import java.util.Locale

/** 파일 크기 문구. 1024 단위, 소수 한 자리(KB 이상), B 는 정수. */
object FileSizeText {
    private const val UNIT = 1024.0

    fun of(bytes: Long): String {
        if (bytes < 0) return ""
        if (bytes < UNIT) return "$bytes B"
        var value = bytes / UNIT
        var unit = "KB"
        if (value >= UNIT) {
            value /= UNIT
            unit = "MB"
        }
        if (value >= UNIT) {
            value /= UNIT
            unit = "GB"
        }
        return String.format(Locale.ROOT, "%.1f %s", value, unit)
    }
}
