package com.example.memberservice.member.dto

/** 즐겨찾기·숨김·차단 켜기/끄기 요청 본문: `{"on": true}`. */
data class FriendFlagDto(var on: Boolean = false)
