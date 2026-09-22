package com.example.memberservice.member.service

import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto
import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.global.exception.ErrorCode
import com.example.memberservice.member.dto.ResponseFriendDto
import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.repository.FriendFilter
import com.example.memberservice.member.repository.FriendSort
import com.example.memberservice.member.repository.MemberFriendRepository
import com.example.memberservice.member.repository.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 친구 관계와 "내가 정한 친구 이름"(별칭). 별칭은 보는 사람마다 다르므로 항상 userId(나) 기준으로 읽는다. */
@Service
@Transactional
class MemberFriendService(
    private val memberRepository: MemberRepository,
    private val memberFriendRepository: MemberFriendRepository,
) {

    /** 친구 추가. 이미 친구면 새 행을 만들지 않고 기존 행을 돌려준다. 별칭 초기값은 상대의 현재 이름. */
    fun addFriend(userId: String, email: String): ResponseFriendDto {
        val me = findMe(userId)
        val friend = memberRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND, email) }

        val memberFriend = memberFriendRepository.findByMemberIdAndFriendId(me.id!!, friend.id!!)
            .orElseGet { memberFriendRepository.save(MemberFriend.of(me, friend)) }
        return ResponseFriendDto.from(memberFriend)
    }

    /** 친구 목록 한 페이지. filter 기본값(NORMAL)은 숨김·차단한 친구를 뺀다. */
    @Transactional(readOnly = true)
    fun getFriendsPage(userId: String, filter: FriendFilter?, sort: FriendSort, pageable: Pageable): Page<ResponseFriendDto> {
        val me = findMe(userId)
        return memberFriendRepository.findPage(me.id!!, filter, sort, pageable).map(ResponseFriendDto::from)
    }

    /** 친구 한 명. 친구가 아니면 USERID_NOT_FOUND_ERROR(컨트롤러가 404 로 바꾼다). */
    @Transactional(readOnly = true)
    fun getFriend(userId: String, friendMemberId: Long): ResponseFriendDto =
        ResponseFriendDto.from(findFriendRow(userId, friendMemberId))

    /** 즐겨찾기 켜기/끄기. 차단한 친구면 400(엔티티가 던진다). */
    fun setFavorite(userId: String, friendMemberId: Long, on: Boolean): ResponseFriendDto {
        val memberFriend = findFriendRow(userId, friendMemberId)
        memberFriend.updateFavorite(on)
        return ResponseFriendDto.from(memberFriend)
    }

    /** 숨기기/숨김 해제. 해제하면 NORMAL 로 돌아온다. */
    fun setHidden(userId: String, friendMemberId: Long, on: Boolean): ResponseFriendDto {
        val memberFriend = findFriendRow(userId, friendMemberId)
        if (on) {
            memberFriend.hide()
        } else {
            memberFriend.unhide()
        }
        return ResponseFriendDto.from(memberFriend)
    }

    /** 차단/차단 해제. 차단하면 즐겨찾기가 꺼지고, 해제하면 NORMAL 로 돌아온다. */
    fun setBlocked(userId: String, friendMemberId: Long, on: Boolean): ResponseFriendDto {
        val memberFriend = findFriendRow(userId, friendMemberId)
        if (on) {
            memberFriend.block()
        } else {
            memberFriend.unblock()
        }
        return ResponseFriendDto.from(memberFriend)
    }

    /** 내가 차단한 친구들의 userId. 앱이 메시지·알림을 거를 때 쓴다. */
    @Transactional(readOnly = true)
    fun getBlockedUserIds(userId: String): List<String> {
        val me = findMe(userId)
        return memberFriendRepository.findFriendUserIdsByStatus(me.id!!, FriendStatus.BLOCKED)
    }

    /**
     * 나를 차단한 사람들의 userId. ws-service 가 1:1 방에서 전달·푸시를 거를 때 쓴다.
     * 상대가 나를 차단했는지는 내 존재 여부와 무관하게 member_friend 행으로 판정하므로
     * 회원을 먼저 찾지 않는다(없는 userId 면 빈 목록).
     */
    @Transactional(readOnly = true)
    fun getBlockedByUserIds(userId: String): List<String> =
        memberFriendRepository.findMemberUserIdsByFriendUserIdAndStatus(userId, FriendStatus.BLOCKED)

    /** 별칭 변경. 친구가 아니면 USERID_NOT_FOUND_ERROR. 공백 검증은 컨트롤러가 한다. */
    fun renameFriend(userId: String, friendMemberId: Long, name: String): ResponseFriendDto {
        val memberFriend = findFriendRow(userId, friendMemberId)
        memberFriend.rename(name.trim())
        return ResponseFriendDto.from(memberFriend)
    }

    /** friend userId → 별칭. 앱이 채팅 화면과 푸시 알림에서 이름을 치환할 때 쓴다. */
    @Transactional(readOnly = true)
    fun getFriendNames(userId: String): Map<String, String> {
        val me = findMe(userId)
        val names = LinkedHashMap<String, String>()
        for (mf in memberFriendRepository.findAllByMemberIdWithFriend(me.id!!)) {
            names[mf.friend.userId] = mf.friendName
        }
        return names
    }

    @Transactional(readOnly = true)
    fun countFriends(memberId: Long): Long = memberFriendRepository.countByMemberId(memberId)

    /** 백오피스 회원 상세용. 이름순 전체. */
    @Transactional(readOnly = true)
    fun listForAdmin(memberId: Long): List<AdminMemberSummaryDto> =
        memberFriendRepository.findPage(memberId, FriendSort.NAME_ASC, Pageable.unpaged())
            .map(AdminMemberSummaryDto::from)
            .content

    /** 내가 소유한 친구 행. 내 친구가 아니면(남의 행이면) 못 찾으므로 404 가 된다. */
    private fun findFriendRow(userId: String, friendMemberId: Long): MemberFriend {
        val me = findMe(userId)
        return memberFriendRepository.findByMemberIdAndFriendId(me.id!!, friendMemberId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, friendMemberId.toString()) }
    }

    private fun findMe(userId: String): Member =
        memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }
}
