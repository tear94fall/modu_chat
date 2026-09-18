package com.example.memberservice.chat.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 회원 탈퇴 때 chat-service 에 방 정리를 맡긴다. 내부 토큰은 InternalApiFeignConfig 가 붙인다. */
@FeignClient("chat-service")
public interface ChatFeignClient {

    /** 이 회원이 든 모든 방에서 나가고, 비게 된 방은 지운다. 들어 있던 방 PK 목록을 돌려준다. */
    @DeleteMapping("/api-internal/chat/member/{memberId}/rooms")
    List<Long> exitAllChatRooms(@PathVariable("memberId") Long memberId);
}
