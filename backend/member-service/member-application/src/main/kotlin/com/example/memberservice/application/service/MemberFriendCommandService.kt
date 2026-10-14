package com.example.memberservice.application.service

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
import com.example.memberservice.application.config.RwJpaConfig
import com.example.memberservice.application.domain.entity.FriendStatus
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.repository.query.FriendFilter
import com.example.memberservice.application.domain.repository.query.FriendSort
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.usecase.result.FriendResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 친구 쓰기(master)와, 쓰기 직후에 읽히는 조회.
 *
 * "쓰기 직후 읽기": 차단 목록([getBlockedUserIds], [getBlockedByUserIds])은 ws-service·chat-service·앱이 차단 직후에 읽어
 * 캐시(60초)에 담는다 — 레플리카의 옛 값을 읽으면 그동안 차단이 먹지 않는다. 친구 한 명([getFriend])은 프로필 화면이
 * 즐겨찾기·숨김·차단을 바꾼 직후에 읽는다. 앱의 친구 목록([getFriendsPage])과 이름표([getFriendNames])도 master 다 —
 * 앱은 친구 추가·이름 변경·즐겨찾기·숨김·차단 직후에 목록을 다시 받으므로, 복제 지연에 따라 결과가 달라지면 안 된다.
 * 별칭은 보는 사람마다 다르므로 항상 userId(나) 기준으로 읽는다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class MemberFriendCommandService(
    private val memberRwRepository: MemberRwRepository,
    private val memberFriendRwRepository: MemberFriendRwRepository,
) {

    /** 친구 추가. 이미 친구면 새 행을 만들지 않고 기존 행을 돌려준다. 별칭 초기값은 상대의 현재 이름. */
    fun addFriend(userId: String, email: String): FriendResult {
        val me = findMe(userId)
        val friend = memberRwRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND, email) }

        val memberFriend = memberFriendRwRepository.findByMemberIdAndFriendId(me.id!!, friend.id!!)
            .orElseGet { memberFriendRwRepository.save(MemberFriend.of(me, friend)) }
        return FriendResult.from(memberFriend)
    }

    /** 친구 목록 한 페이지. filter 기본값(NORMAL)은 숨김·차단한 친구를 뺀다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getFriendsPage(userId: String, filter: FriendFilter?, sort: FriendSort, pageable: Pageable): Page<FriendResult> {
        val me = findMe(userId)
        return memberFriendRwRepository.findPage(me.id!!, filter, sort, pageable).map(FriendResult::from)
    }

    /** friend userId → 별칭. 앱이 채팅 화면과 푸시 알림에서 이름을 치환할 때 쓴다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getFriendNames(userId: String): Map<String, String> {
        val me = findMe(userId)
        val names = LinkedHashMap<String, String>()
        for (mf in memberFriendRwRepository.findAllByMemberIdWithFriend(me.id!!)) {
            names[mf.friend.userId] = mf.friendName
        }
        return names
    }

    /** 친구 한 명. 내 친구가 아니면 FRIEND_NOT_FOUND_ERROR(404). */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getFriend(userId: String, friendMemberId: Long): FriendResult =
        FriendResult.from(findFriendRow(userId, friendMemberId))

    /** 즐겨찾기 켜기/끄기. 차단한 친구면 400(엔티티가 던진다). */
    fun setFavorite(userId: String, friendMemberId: Long, on: Boolean): FriendResult {
        val memberFriend = findFriendRow(userId, friendMemberId)
        memberFriend.updateFavorite(on)
        return FriendResult.from(memberFriend)
    }

    /** 숨기기/숨김 해제. 해제하면 NORMAL 로 돌아온다. */
    fun setHidden(userId: String, friendMemberId: Long, on: Boolean): FriendResult {
        val memberFriend = findFriendRow(userId, friendMemberId)
        if (on) {
            memberFriend.hide()
        } else {
            memberFriend.unhide()
        }
        return FriendResult.from(memberFriend)
    }

    /** 차단/차단 해제. 차단하면 즐겨찾기가 꺼지고, 해제하면 NORMAL 로 돌아온다. */
    fun setBlocked(userId: String, friendMemberId: Long, on: Boolean): FriendResult {
        val memberFriend = findFriendRow(userId, friendMemberId)
        if (on) {
            memberFriend.block()
        } else {
            memberFriend.unblock()
        }
        return FriendResult.from(memberFriend)
    }

    /** 별칭 변경. 친구가 아니면 FRIEND_NOT_FOUND_ERROR. 공백 검증은 컨트롤러가 한다. */
    fun renameFriend(userId: String, friendMemberId: Long, name: String): FriendResult {
        val memberFriend = findFriendRow(userId, friendMemberId)
        memberFriend.rename(name.trim())
        return FriendResult.from(memberFriend)
    }

    /** 내가 차단한 친구들의 userId. 앱·chat-service 가 메시지·알림을 거를 때 쓴다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getBlockedUserIds(userId: String): List<String> {
        val me = findMe(userId)
        return memberFriendRwRepository.findFriendUserIdsByStatus(me.id!!, FriendStatus.BLOCKED)
    }

    /**
     * 나를 차단한 사람들의 userId. ws-service 가 1:1 방에서 전달·푸시를 거를 때 쓴다.
     * 상대가 나를 차단했는지는 내 존재 여부와 무관하게 member_friend 행으로 판정하므로
     * 회원을 먼저 찾지 않는다(없는 userId 면 빈 목록).
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getBlockedByUserIds(userId: String): List<String> =
        memberFriendRwRepository.findMemberUserIdsByFriendUserIdAndStatus(userId, FriendStatus.BLOCKED)

    /** 내가 소유한 친구 행. 내 친구가 아니면(남의 행이면) 못 찾으므로 404 가 된다. */
    private fun findFriendRow(userId: String, friendMemberId: Long): MemberFriend {
        val me = findMe(userId)
        return memberFriendRwRepository.findByMemberIdAndFriendId(me.id!!, friendMemberId)
            .orElseThrow { CustomException(ErrorCode.FRIEND_NOT_FOUND_ERROR, friendMemberId.toString()) }
    }

    private fun findMe(userId: String): Member =
        memberRwRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }
}
