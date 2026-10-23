package com.example.chatservice.application.usecase.result

import java.time.LocalDate

/** 백오피스 홈 "오늘의 숫자" 중 채팅 부분. [days] 는 오래된 날부터 오늘까지. */
data class ChatStatsResult(val zone: String, val days: List<ChatStatsDay>)

/** 그 날 보낸 메시지 수(시스템 안내 제외)와 만들어진 방 수. */
data class ChatStatsDay(val date: LocalDate, val messages: Int, val roomsCreated: Int)
