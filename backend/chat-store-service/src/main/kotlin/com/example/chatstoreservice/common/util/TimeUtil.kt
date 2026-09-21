package com.example.chatstoreservice.common.util

import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.TimeZone

object TimeUtil {

    @JvmStatic
    fun converDateToLocalDateTime(date: Long?): LocalDateTime =
        if (date == null) LocalDateTime.now() else LocalDateTime.ofInstant(Instant.ofEpochMilli(date), TimeZone.getDefault().toZoneId())

    @JvmStatic
    fun converDateToLocalDateTime(date: Date?): LocalDateTime =
        date?.toInstant()?.atZone(TimeZone.getDefault().toZoneId())?.toLocalDateTime() ?: LocalDateTime.now()
}
