package com.example.memberservice.application.domain.repository.query

import com.example.memberservice.application.domain.entity.FriendStatus
import com.example.memberservice.application.domain.entity.MemberFriend
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** 친구 QueryDSL 질의. ro/rw 저장소가 각자의 JPAQueryFactory 로 같은 구현([MemberFriendQueries])을 쓴다. */
interface MemberFriendQueryOperations {

    /** 내 친구 한 페이지(상태 구분 없이 전부). 백오피스처럼 숨김·차단까지 봐야 하는 곳이 쓴다. */
    fun findPage(memberId: Long, sort: FriendSort, pageable: Pageable): Page<MemberFriend>

    /** 내 친구 한 페이지. 친구 Member 를 fetch join 한다. 정렬은 [FriendSort], Pageable 은 offset/limit 만 쓴다. */
    fun findPage(memberId: Long, filter: FriendFilter?, sort: FriendSort, pageable: Pageable): Page<MemberFriend>

    /**
     * 백오피스 친구 탭 한 페이지(내용만). 친구 Member 를 fetch join 하고 [FriendSort.adminOrders] 로 정렬한다.
     * 전체 개수는 [countForAdmin] 결과에서 꺼내 쓰므로 여기서 count 질의를 하지 않는다.
     */
    fun findAdminPageContent(memberId: Long, filter: AdminFriendFilter, pageable: Pageable): List<MemberFriend>

    /** 이 회원이 소유한 친구 행의 필터별 수. status·favorite 로 묶은 질의 한 번이다. */
    fun countForAdmin(memberId: Long): FriendCounts

    /** 내 친구 전부(별칭 맵용). 친구 Member 를 fetch join 한다. 숨김·차단도 포함한다. */
    fun findAllByMemberIdWithFriend(memberId: Long): List<MemberFriend>

    /** 해당 상태인 친구들의 userId. 차단 목록을 앱에 내려줄 때 쓴다. */
    fun findFriendUserIdsByStatus(memberId: Long, status: FriendStatus): List<String>

    /**
     * 역방향: 이 userId 를 해당 상태로 등록한 사람들의 userId.
     * (member_friend 에서 friend.userId = friendUserId 인 행의 member.userId)
     * ws-service 가 "누가 나를 차단했나"를 물어볼 때 쓴다. member 를 먼저 찾지 않고
     * userId 로 바로 조인하므로 없는 userId 면 빈 목록이다.
     */
    fun findMemberUserIdsByFriendUserIdAndStatus(friendUserId: String, status: FriendStatus): List<String>
}
