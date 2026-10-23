package com.example.chatservice.api.admin

import java.time.DateTimeException
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** 백오피스가 보내는 표시 시간대(IANA 이름). 모르는 이름은 400. */
object AdminZone {
    fun parse(zone: String): ZoneId =
        try {
            ZoneId.of(zone)
        } catch (e: DateTimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "모르는 시간대입니다: $zone")
        }
}
