package com.example.memberservice.application.service

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.domain.repository.query.AdminFriendFilter
import com.example.memberservice.application.domain.repository.query.FriendSort
import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.domain.repository.query.ServiceFilter
import com.example.memberservice.application.domain.repository.ro.MemberFriendRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberServiceUsageRoRepository
import com.example.memberservice.application.domain.repository.ro.StaffRoRepository
import com.example.memberservice.application.usecase.result.AdminFriendPageResult
import com.example.memberservice.application.usecase.result.AdminFriendResult
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import com.example.memberservice.application.usecase.result.MemberResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 둘러보기(replica): 남의 프로필 보기, 회원 검색, 여러 명 조회, 백오피스 목록·상세·친구 탭.
 * 복제는 비동기라 방금 쓴 값은 아직 없을 수 있다 — 쓰기 직후에 읽히는 조회는 [MemberCommandService] 에 있다.
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class MemberQueryService(
    private val memberRoRepository: MemberRoRepository,
    private val memberFriendRoRepository: MemberFriendRoRepository,
    private val staffRoRepository: StaffRoRepository,
    private val usageRoRepository: MemberServiceUsageRoRepository,
) {

    fun getMemberById(id: Long): MemberResult =
        memberRoRepository.findById(id).map(MemberResult::from)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id) }

    /** 친구 추가 화면의 검색. 이메일이 정확히 같은 회원. 없으면 빈 목록. */
    fun findFriend(email: String): List<MemberResult> {
        if (!memberRoRepository.existsByEmail(email)) {
            return emptyList()
        }
        return memberRoRepository.findAllByEmail(email).map(MemberResult::from)
    }

    fun findMembers(userIds: List<String>): List<MemberResult> =
        memberRoRepository.findAllByUserIdIn(userIds).map(MemberResult::from)

    fun findMembersById(ids: List<Long>): List<MemberResult> =
        memberRoRepository.findAllById(ids).map(MemberResult::from)

    /**
     * 백오피스 검색. keyword 가 비면 전체.
     * 정렬은 [MemberSort] 로 정한다(기본 이름 가나다순, 한글 이름 먼저). Pageable 의 sort 는 쓰지 않는다 —
     * "한글 먼저" 는 컬럼 하나로 표현할 수 없어 QueryDSL CASE 로 만들어야 한다.
     */
    fun searchMembers(
        keyword: String?,
        sort: MemberSort?,
        pageable: Pageable,
        service: ServiceFilter? = null,
    ): Page<AdminMemberSummaryResult> {
        val page = memberRoRepository.searchForAdmin(keyword, sort ?: MemberSort.DEFAULT, pageable, service = service)
        val staff = staffPermissionsOf(page.content.mapNotNull { it.id })
        val services = servicesOf(page.content.map { it.userId })
        return page.map {
            AdminMemberSummaryResult.from(it)
                .copy(staffPermissions = staff[it.id].orEmpty(), services = services[it.userId].orEmpty())
        }
    }

    /** 백오피스 상세: 회원 + 친구 목록(이름순) + 직원 권한 + 이용 서비스. */
    fun getMemberDetail(id: Long): AdminMemberDetailResult {
        val member = memberRoRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id.toString()) }
        return AdminMemberDetails.of(
            member,
            memberFriendRoRepository.findPage(id, FriendSort.NAME_ASC, Pageable.unpaged()).content,
            staffRoRepository::findAllByMemberIdIn,
            usageRoRepository.findAllByUserId(member.userId),
        )
    }

    /**
     * 백오피스 회원 상세의 친구 탭. 이 회원이 소유한 친구 행(상세의 friends 와 같은 방향)을 필터·페이지로 자른다.
     * 질의: 회원 존재 확인 1 + 상태별 수 1(전체 개수도 여기서 꺼낸다) + 페이지 1 + 직원 권한 1 + 이용 서비스 1.
     * 없는 회원이면 404.
     */
    fun getMemberFriends(id: Long, filter: AdminFriendFilter, pageable: Pageable): AdminFriendPageResult {
        if (!memberRoRepository.existsById(id)) {
            throw CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id)
        }
        val counts = memberFriendRoRepository.countForAdmin(id)
        val total = counts.of(filter)
        val rows = if (pageable.isPaged && pageable.offset >= total) {
            emptyList()
        } else {
            memberFriendRoRepository.findAdminPageContent(id, filter, pageable)
        }
        val staff = staffPermissionsOf(rows.mapNotNull { it.friend.id })
        val services = servicesOf(rows.map { it.friend.userId })
        val size = pageable.pageSize
        return AdminFriendPageResult(
            content = rows.map {
                AdminFriendResult.from(it, staff[it.friend.id].orEmpty(), services[it.friend.userId].orEmpty())
            },
            totalElements = total,
            totalPages = if (total == 0L) 0 else ((total + size - 1) / size).toInt(),
            number = pageable.pageNumber,
            size = size,
            counts = counts,
        )
    }

    /** memberId → 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL 순). 직원이 아닌 회원은 맵에 없다. */
    private fun staffPermissionsOf(memberIds: Collection<Long>): Map<Long, List<StaffPermission>> =
        if (memberIds.isEmpty()) {
            emptyMap()
        } else {
            AdminMemberDetails.permissionsByMemberId(staffRoRepository.findAllByMemberIdIn(memberIds))
        }

    /** userId → 이용 서비스(CHAT, COMMERCE 순). 기록이 없는 회원은 맵에 없다. */
    private fun servicesOf(userIds: Collection<String>): Map<String, List<ModuService>> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            usageRoRepository.findAllByUserIdIn(userIds)
                .groupBy({ it.userId }, { it.service })
                .mapValues { (_, services) -> services.distinct().sorted() }
        }
}
