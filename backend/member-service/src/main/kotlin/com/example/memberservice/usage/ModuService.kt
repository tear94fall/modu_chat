package com.example.memberservice.usage

/**
 * 회원이 쓰는 모두 서비스. auth-service 가 토큰을 발급할 때 클라이언트로 정해진다(modu.member.usage-clients).
 * 선언 순서가 화면·응답의 정렬 순서다(CHAT, COMMERCE).
 */
enum class ModuService { CHAT, COMMERCE }

/** 백오피스 회원 목록의 이용 서비스 필터. NONE 은 이용 기록이 하나도 없는 회원이다. */
enum class ServiceFilter { CHAT, COMMERCE, BOTH, NONE }
