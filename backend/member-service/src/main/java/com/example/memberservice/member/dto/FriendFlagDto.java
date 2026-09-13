package com.example.memberservice.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 즐겨찾기·숨김·차단 켜기/끄기 요청 본문: {@code {"on": true}}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendFlagDto {
    private boolean on;
}
