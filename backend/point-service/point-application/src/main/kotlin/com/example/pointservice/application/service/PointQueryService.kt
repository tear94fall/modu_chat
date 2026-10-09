package com.example.pointservice.application.service

import com.example.pointservice.application.config.RoJpaConfig
import com.example.pointservice.application.domain.repository.ro.PointAccountRoRepository
import com.example.pointservice.application.domain.repository.ro.PointTransactionRoRepository
import com.example.pointservice.application.usecase.command.TransactionRef
import com.example.pointservice.application.usecase.result.PointAccountResult
import com.example.pointservice.application.usecase.result.PointRefResult
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
class PointQueryService(
    private val accountRoRepository: PointAccountRoRepository,
    private val transactionRoRepository: PointTransactionRoRepository,
) {

    fun accounts(pageable: Pageable): Page<PointAccountResult> =
        accountRoRepository.findAll(pageable).map { PointAccountResult.from(it) }

    fun accountsOf(userIds: Collection<String>, pageable: Pageable): Page<PointAccountResult> =
        accountRoRepository.findByUserIdIn(userIds, pageable).map { PointAccountResult.from(it) }

    /**
     * (userId, refId) 쌍 중 원장에 있는 줄만 돌려준다(커머스 야간 대사). 조금 늦어도 되는 조회라 레플리카에서 읽는다.
     * IN 목록이 너무 길어지지 않게 [REF_CHUNK] 쌍씩 나눠 묻고, 두 IN 의 교차곱으로 딸려 온 줄은 요청한 쌍으로 거른다.
     */
    fun transactionsByRefs(refs: Collection<TransactionRef>): List<PointRefResult> {
        require(refs.size <= MAX_REFS) { "refs 는 최대 $MAX_REFS 개" }
        return refs.distinct().chunked(REF_CHUNK).flatMap { chunk ->
            val wanted = chunk.toSet()
            transactionRoRepository.findByUserIdInAndRefIdIn(chunk.map { it.userId }.toSet(), chunk.map { it.refId }.toSet())
                .filter { TransactionRef(it.userId, it.refId ?: return@filter false) in wanted }
                .map { PointRefResult.from(it) }
        }
    }

    companion object {
        const val MAX_REFS = 500
        const val REF_CHUNK = 100
    }
}
