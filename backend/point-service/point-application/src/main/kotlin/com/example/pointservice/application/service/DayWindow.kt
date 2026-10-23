package com.example.pointservice.application.service

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 백오피스 통계의 "하루" — 표시 시간대([zone])의 달력 날짜로 묶는 [days]일 창(오늘 포함, 오래된 날부터).
 * 저장된 시각은 [storedZone] 의 LocalDateTime 이다(대부분 UTC). 조회는 [fromStored, toStored) 한 번으로 받고 여기서 날짜로 나눈다 —
 * DB 의 시간대 변환(CONVERT_TZ 는 tz 테이블이 필요)에 기대지 않는다.
 */
class DayWindow(
    val zone: ZoneId,
    days: Int,
    clock: Clock,
    private val storedZone: ZoneId = ZoneOffset.UTC,
) {
    val dates: List<LocalDate>
    val fromStored: LocalDateTime
    val toStored: LocalDateTime

    init {
        val n = days.coerceIn(1, MAX_DAYS)
        val today = clock.instant().atZone(zone).toLocalDate()
        dates = (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
        fromStored = toStored(dates.first())
        toStored = toStored(today.plusDays(1))
    }

    /** [zone] 의 그 날 0시를 저장 시간대의 LocalDateTime 으로. */
    fun toStored(date: LocalDate): LocalDateTime = date.atStartOfDay(zone).withZoneSameInstant(storedZone).toLocalDateTime()

    /** 저장된 시각이 [zone] 에서 어느 날짜인지. 창 밖이면 null. */
    fun dateOf(stored: LocalDateTime): LocalDate? =
        stored.atZone(storedZone).withZoneSameInstant(zone).toLocalDate().takeIf { it in dates }

    fun <T> byDate(items: Iterable<T>, at: (T) -> LocalDateTime): Map<LocalDate, List<T>> =
        items.groupBy { dateOf(at(it)) }.filterKeys { it != null }.mapKeys { it.key!! }

    companion object {
        const val MAX_DAYS = 92
    }
}
