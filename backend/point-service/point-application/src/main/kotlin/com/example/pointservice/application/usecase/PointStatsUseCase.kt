package com.example.pointservice.application.usecase

import com.example.pointservice.application.service.PointStatsQueryService
import com.example.pointservice.application.usecase.result.PointStatsResult
import java.time.ZoneId
import org.springframework.stereotype.Component

/** 백오피스 홈 "오늘의 숫자"의 포인트 부분. 전부 replica. */
@Component
class PointStatsUseCase(private val pointStatsQueryService: PointStatsQueryService) {
    fun stats(days: Int, zone: ZoneId): PointStatsResult = pointStatsQueryService.stats(days, zone)
}
