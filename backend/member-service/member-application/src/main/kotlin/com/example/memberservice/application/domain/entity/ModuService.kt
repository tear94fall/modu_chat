package com.example.memberservice.application.domain.entity

/**
 * 회원이 쓰는 모두 서비스. auth-service 가 토큰을 발급할 때 클라이언트로 정해진다(modu.member.usage-clients).
 * 선언 순서가 화면·응답의 정렬 순서다(CHAT, COMMERCE).
 */
enum class ModuService { CHAT, COMMERCE }
