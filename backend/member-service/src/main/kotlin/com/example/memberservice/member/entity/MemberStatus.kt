package com.example.memberservice.member.entity

/** 회원 상태. 탈퇴해도 행은 남긴다 — 채팅 기록이 userId 로 이어져 있어서다. */
enum class MemberStatus {
    ACTIVE,
    WITHDRAWN,
}
