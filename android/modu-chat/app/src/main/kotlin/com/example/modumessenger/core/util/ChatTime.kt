package com.example.modumessenger.core.util

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 서버가 주고받는 채팅 시각(`yyyy-MM-dd HH:mm:ss`) 파싱/포맷.
 *
 * 이 값은 **UTC 벽시계 시각**이다. ws-service 가 앱이 보낸 시각을 UTC 컨테이너 시계로 덮어써서 저장하므로, 저장된 채팅 시각과
 * 방의 마지막 채팅 시각은 모두 UTC 다. 예전에는 이 값을 그대로 보여 줘 한국 시간보다 9시간 늦게 보였다.
 * 그래서 보낼 때는 UTC 로 만들고([now]), 화면에 보일 때만 폰 시간대로 바꾼다([local]).
 */
object ChatTime {

    const val PATTERN = "yyyy-MM-dd HH:mm:ss"

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN)
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
    private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
    private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. M. d.", Locale.KOREA)

    /** 서버 값을 그대로(UTC 벽시계) 읽는다. 정렬·비교용. 화면에는 [local] 을 쓴다. */
    fun parse(value: String?): LocalDateTime? =
        runCatching { LocalDateTime.parse(value?.trim(), formatter) }.getOrNull()

    fun format(time: LocalDateTime): String = time.format(formatter)

    /** 보낼 채팅 시각. 서버와 같은 UTC 로 만든다(서버는 1분 안의 과거 시각이면 그대로 둔다). */
    fun now(clock: Clock = Clock.systemUTC()): String = format(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)))

    /** UTC 서버 시각을 [zone] 의 시각으로. 파싱에 실패하면 null. */
    fun local(value: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? =
        parse(value)?.atOffset(ZoneOffset.UTC)?.atZoneSameInstant(zone)?.toLocalDateTime()

    fun localDate(value: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDate? = local(value, zone)?.toLocalDate()

    /**
     * 말풍선·방 목록에 쓰는 짧은 시각(`오전 10:14`). 파싱에 실패하면 빈 문자열.
     * 말풍선 묶음(HEADER/BODY/TAIL)은 "같은 발신자 + 같은 날짜 + 같은 짧은 시각" 으로 판단한다.
     */
    fun shortTime(value: String?, locale: Locale = Locale.KOREA, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = local(value, zone) ?: return ""
        return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }

    /** 채팅방의 날짜 구분선(`2026년 9월 24일 목요일`). 파싱에 실패하면 빈 문자열. */
    fun dayDivider(value: String?, zone: ZoneId = ZoneId.systemDefault()): String =
        local(value, zone)?.format(dayFormatter).orEmpty()

    /**
     * 채팅방 목록의 마지막 메시지 시각. 오늘이면 `오후 8:40`, 어제면 `어제`, 올해면 `9월 1일`, 그 이전이면 `2025. 12. 31.`.
     * 파싱에 실패하면 빈 문자열.
     */
    fun listTime(
        value: String?,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val time = local(value, zone) ?: return ""
        val date = time.toLocalDate()
        return when {
            date == today -> shortTime(value, Locale.KOREA, zone)
            date == today.minusDays(1) -> "어제"
            date.year == today.year -> date.format(monthDayFormatter)
            else -> date.format(fullDateFormatter)
        }
    }
}
