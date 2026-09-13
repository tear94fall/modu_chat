package com.example.modumessenger.core.util

/**
 * `GET …/friends?filter=` 값. 서버가 아는 문자열만 보낸다(모르는 값이면 400).
 *
 * - [NORMAL] 숨김·차단이 아닌 친구(즐겨찾기 포함) — 기본값
 * - [FAVORITE] 그중 즐겨찾기만
 * - [HIDDEN] 숨긴 친구
 * - [BLOCKED] 차단한 친구
 */
enum class FriendFilter(val value: String) {
    NORMAL("normal"),
    FAVORITE("favorite"),
    HIDDEN("hidden"),
    BLOCKED("blocked"),
}
