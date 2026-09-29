package com.example.memberservice.application.domain.repository.query

/** 백오피스 회원 목록의 이용 서비스 필터. NONE 은 이용 기록이 하나도 없는 회원이다. */
enum class ServiceFilter { CHAT, COMMERCE, BOTH, NONE }
