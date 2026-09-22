package com.example.memberservice.member.entity

/**
 * 내가 친구 한 명에게 매긴 상태. 숨김과 차단은 배타라서 즐겨찾기(boolean)와 달리 하나의 컬럼으로 둔다.
 * NORMAL 이 기본이고, 숨김 해제·차단 해제는 모두 NORMAL 로 돌아온다.
 */
enum class FriendStatus {
    NORMAL, HIDDEN, BLOCKED
}
