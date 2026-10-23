package com.example.chatservice.application.usecase

import com.example.chatservice.application.service.ChatStatsQueryService
import com.example.chatservice.application.usecase.result.ChatStatsResult
import java.time.ZoneId
import org.springframework.stereotype.Component

/** 백오피스 홈 "오늘의 숫자"의 채팅 부분. 전부 replica. */
@Component
class ChatStatsUseCase(private val chatStatsQueryService: ChatStatsQueryService) {
    fun stats(days: Int, zone: ZoneId): ChatStatsResult = chatStatsQueryService.stats(days, zone)
}
