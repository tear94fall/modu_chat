package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.repository.query.FriendFilter
import com.example.memberservice.application.domain.repository.query.FriendSort
import com.example.memberservice.application.service.MemberFriendCommandService
import com.example.memberservice.application.usecase.result.FriendResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 친구 유스케이스. userId 는 항상 "나"다(앱 API 는 AuthUserInterceptor 가 토큰의 주인과 같은지 확인한다).
 * 앱의 친구 읽기(목록·이름표·친구 한 명·차단 목록)는 모두 master 다 — 앱이 친구를 바꾼 직후에 다시 읽는다.
 * 레플리카로 읽는 친구 조회는 백오피스 친구 탭뿐이다(MemberAdminUseCase).
 */
@Component
class FriendUseCase(
    private val memberFriendCommandService: MemberFriendCommandService,
) {

    fun getFriendsPage(userId: String, filter: FriendFilter?, sort: FriendSort, pageable: Pageable): Page<FriendResult> =
        memberFriendCommandService.getFriendsPage(userId, filter, sort, pageable)

    fun getFriendNames(userId: String): Map<String, String> = memberFriendCommandService.getFriendNames(userId)

    fun getFriend(userId: String, friendMemberId: Long): FriendResult = memberFriendCommandService.getFriend(userId, friendMemberId)

    fun addFriend(userId: String, email: String): FriendResult = memberFriendCommandService.addFriend(userId, email)

    fun renameFriend(userId: String, friendMemberId: Long, name: String): FriendResult =
        memberFriendCommandService.renameFriend(userId, friendMemberId, name)

    fun setFavorite(userId: String, friendMemberId: Long, on: Boolean): FriendResult =
        memberFriendCommandService.setFavorite(userId, friendMemberId, on)

    fun setHidden(userId: String, friendMemberId: Long, on: Boolean): FriendResult =
        memberFriendCommandService.setHidden(userId, friendMemberId, on)

    fun setBlocked(userId: String, friendMemberId: Long, on: Boolean): FriendResult =
        memberFriendCommandService.setBlocked(userId, friendMemberId, on)

    /** userId 가 차단한 사람들. 앱과 chat-service(60초 캐시)가 차단 직후에 읽는다. */
    fun getBlockedUserIds(userId: String): List<String> = memberFriendCommandService.getBlockedUserIds(userId)

    /** userId 를 차단한 사람들(역방향). ws-service(60초 캐시)가 차단 직후에 읽는다. */
    fun getBlockedByUserIds(userId: String): List<String> = memberFriendCommandService.getBlockedByUserIds(userId)
}
