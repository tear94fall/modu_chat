package com.example.pointservice.application.service

import com.example.pointservice.application.config.RoJpaConfig
import com.example.pointservice.application.domain.repository.ro.PointAccountRoRepository
import com.example.pointservice.application.usecase.result.PointAccountResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 계정 둘러보기(replica). 백오피스 목록·검색처럼 조금 늦어도 되는 조회만 둔다.
 * 쓰기 직후에 읽는 잔액·원장·계정 상세는 [PointCommandService] 가 master 로 읽는다.
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class PointQueryService(private val accountRoRepository: PointAccountRoRepository) {

    fun accounts(pageable: Pageable): Page<PointAccountResult> =
        accountRoRepository.findAll(pageable).map { PointAccountResult.from(it) }

    fun accountsOf(userIds: Collection<String>, pageable: Pageable): Page<PointAccountResult> =
        accountRoRepository.findByUserIdIn(userIds, pageable).map { PointAccountResult.from(it) }
}
