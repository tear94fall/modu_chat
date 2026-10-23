package com.example.pointservice.application.service

import com.example.pointservice.application.config.RoJpaConfig
import com.example.pointservice.application.domain.entity.PointTransactionType
import com.example.pointservice.application.domain.repository.ro.PointTransactionRoRepository
import com.example.pointservice.application.usecase.result.PointStatsDay
import com.example.pointservice.application.usecase.result.PointStatsResult
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 백오피스 홈의 포인트 숫자(replica). 원장(point_transaction)의 created_date 는 한국 시간(ClockConfig 의 시계)이라
 * 저장 시간대를 clock 의 시간대로 두고 표시 시간대의 날짜로 묶는다.
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class PointStatsQueryService(
    private val transactions: PointTransactionRoRepository,
    private val clock: Clock,
) {
    fun stats(days: Int, zone: ZoneId): PointStatsResult {
        val window = DayWindow(zone, days, clock, storedZone = clock.zone)
        val rows = transactions.findTypeAmountCreatedBetween(window.fromStored, window.toStored)
            .map { Row(it[0] as PointTransactionType, (it[1] as Number).toLong(), it[2] as LocalDateTime) }
        val byDate = window.byDate(rows) { it.at }
        return PointStatsResult(
            zone.id,
            window.dates.map { date ->
                val sums = byDate[date].orEmpty().groupBy { it.type }.mapValues { (_, r) -> r.sumOf { abs(it.amount) } }
                PointStatsDay(
                    date,
                    earned = sums[PointTransactionType.EARN] ?: 0L,
                    spent = sums[PointTransactionType.SPEND] ?: 0L,
                    refunded = sums[PointTransactionType.REFUND] ?: 0L,
                    adjusted = sums[PointTransactionType.ADJUST] ?: 0L,
                )
            },
        )
    }

    private data class Row(val type: PointTransactionType, val amount: Long, val at: LocalDateTime)
}
