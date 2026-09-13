package com.example.modumessenger.core.model

/**
 * 친구 한 명의 상태. 숨김과 차단은 배타다(서버 `member_friend.status` 와 문자열이 같아야 한다).
 * 매핑은 [com.example.modumessenger.data.dto.friendStatusOf] 가 한다.
 */
enum class FriendStatus { NORMAL, HIDDEN, BLOCKED }
