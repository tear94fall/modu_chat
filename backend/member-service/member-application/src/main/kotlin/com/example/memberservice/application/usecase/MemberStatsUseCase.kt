package com.example.memberservice.application.usecase

import com.example.memberservice.application.service.MemberStatsQueryService
import com.example.memberservice.application.usecase.result.MemberStatsResult
import java.time.ZoneId
import org.springframework.stereotype.Component

/** 백오피스 홈 "오늘의 숫자"의 회원 부분. 전부 replica. */
@Component
class MemberStatsUseCase(private val memberStatsQueryService: MemberStatsQueryService) {
    fun stats(days: Int, zone: ZoneId): MemberStatsResult = memberStatsQueryService.stats(days, zone)
}
