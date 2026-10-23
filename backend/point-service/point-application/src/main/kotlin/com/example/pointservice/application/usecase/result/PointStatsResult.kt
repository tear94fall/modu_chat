package com.example.pointservice.application.usecase.result

import java.time.LocalDate

/** 백오피스 홈 "오늘의 숫자" 중 포인트 부분. [days] 는 오래된 날부터 오늘까지, 금액은 포인트(P). */
data class PointStatsResult(val zone: String, val days: List<PointStatsDay>)

/** 그 날 적립(EARN)·사용(SPEND)·환불(REFUND)·조정(ADJUST)된 포인트 합(절댓값). */
data class PointStatsDay(val date: LocalDate, val earned: Long, val spent: Long, val refunded: Long, val adjusted: Long)
