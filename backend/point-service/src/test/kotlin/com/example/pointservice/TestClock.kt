package com.example.pointservice

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** 테스트가 날짜를 넘길 수 있는 시계. 하루 상한이 다음 날 풀리는지 볼 때 쓴다. */
class TestClock(@Volatile var now: Instant = Instant.parse("2026-09-23T03:00:00Z")) : Clock() {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
    fun plusDays(days: Long) { now = now.plusSeconds(days * 86_400) }
}

@TestConfiguration
class TestClockConfig {
    @Bean
    @Primary
    fun testClock(): TestClock = TestClock()
}
