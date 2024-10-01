package com.example.chatstoreservice.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public final class TimeUtil {

    public static LocalDateTime converDateToLocalDateTime(Long date) {
        return date == null ? LocalDateTime.now() : LocalDateTime.ofInstant(Instant.ofEpochMilli(date), TimeZone.getDefault().toZoneId());
    }

    public static LocalDateTime converDateToLocalDateTime(Date date) {
        return date == null ? LocalDateTime.now() : date.toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDateTime();
    }
}
