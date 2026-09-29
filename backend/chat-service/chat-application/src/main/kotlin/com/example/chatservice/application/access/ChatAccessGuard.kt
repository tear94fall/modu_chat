package com.example.chatservice.application.access

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.service.ChatRoomCommandService
import org.springframework.stereotype.Component

/**
 * 앱 API(`/api-public/`**)의 본인 확인·방 멤버 확인을 한 곳에서 한다. 유스케이스가 일을 시작하기 전에 부른다.
 * 내부(`/api-internal`)·백오피스(`/api-admin`) 유스케이스는 이 검사를 거치지 않는다(InternalApiFilter 가 막는다).
 */
@Component
class ChatAccessGuard(
    private val callerIdentityCache: CallerIdentityCache,
    private val chatRoomCommandService: ChatRoomCommandService,
) {

    /** 게이트웨이가 넣어 준 X-Auth-User-Id 의 주인. 헤더가 없으면 403. */
    fun caller(authUserId: String?): Caller {
        if (authUserId.isNullOrBlank()) throw CustomException(ErrorCode.FORBIDDEN)
        return Caller(authUserId, callerIdentityCache.memberIdOf(authUserId))
    }

    /** 경로·본문의 id(userId 또는 회원 id)가 요청한 사람 자신이어야 한다. */
    fun requireSelf(caller: Caller, id: String?) {
        if (!caller.isSelf(id)) throw CustomException(ErrorCode.FORBIDDEN)
    }

    /**
     * 방 멤버만 방의 대화·멤버를 볼 수 있다. 멤버 행은 방금 만들어졌거나(방 생성·초대) 방금 지워졌을 수 있어 master 에서 읽는다.
     * 없는 방은 지킬 것이 없으므로 통과시킨다 — 그 뒤는 API 마다 하던 대로다(방 조회는 404, 대화 조회는 빈 목록).
     */
    fun requireRoomMember(caller: Caller, roomId: String): RoomMembership {
        val membership = chatRoomCommandService.membership(roomId)
        if (membership.exists && !membership.has(caller.memberId)) {
            throw CustomException(ErrorCode.NOT_CHAT_ROOM_MEMBER, roomId)
        }
        return membership
    }
}
