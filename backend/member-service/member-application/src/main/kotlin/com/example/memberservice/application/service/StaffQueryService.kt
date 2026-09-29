package com.example.memberservice.application.service

import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.repository.query.MemberSort
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.ro.StaffRoRepository
import com.example.memberservice.application.usecase.result.StaffMemberSummaryResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 모두 인터널의 회원 목록(replica). 둘러보기라 레플리카를 읽는다. */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class StaffQueryService(
    private val staffRoRepository: StaffRoRepository,
    private val memberRoRepository: MemberRoRepository,
) {

    fun searchMembers(keyword: String?, sort: MemberSort, pageable: Pageable, staffOnly: Boolean): Page<StaffMemberSummaryResult> {
        val ids = if (staffOnly) staffRoRepository.findAll().map { it.memberId } else null
        val page = memberRoRepository.searchForAdmin(keyword, sort, pageable, ids)
        val staffById = staffRoRepository.findAllByMemberIdIn(page.content.mapNotNull { it.id }).associateBy { it.memberId }
        return page.map { StaffMemberSummaryResult.of(it, staffById[it.id]) }
    }
}
