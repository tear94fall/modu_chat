package com.example.wsservice.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeUtil {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * 앱이 보낸 전송 시각을 서버 시각으로 보정한다. 미래이거나 1분보다 오래된 시각은 지금으로 바꾸고,
     * 지난 1분 안이면 앱이 보낸 값을 그대로 둔다(옛 자바와 같은 규칙).
     */
    @JvmStatic
    fun calculateTime(sendTime: String): String {
        val chatTime = LocalDateTime.parse(sendTime, FORMATTER)
        val currentTime = LocalDateTime.now()
        val currentTimeStr = currentTime.format(FORMATTER)
        if (!chatTime.isBefore(currentTime)) return currentTimeStr
        if (chatTime.isBefore(currentTime.minusMinutes(1))) return currentTimeStr
        return sendTime
    }
}
