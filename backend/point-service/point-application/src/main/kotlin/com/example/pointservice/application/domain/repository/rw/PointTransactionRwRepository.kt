package com.example.pointservice.application.domain.repository.rw

import com.example.pointservice.application.config.RwRepository
import com.example.pointservice.application.domain.entity.PointTransaction
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PointTransactionRwRepository : RwRepository<PointTransaction, Long> {

    fun findByUserIdOrderByIdDesc(userId: String, pageable: Pageable): Page<PointTransaction>

    fun existsByUserIdAndRefId(userId: String, refId: String): Boolean

    /** (user_id, ref_id) 유니크 키로 한 줄. 사용 취소가 원래 사용 줄을 찾을 때 쓴다. */
    fun findByUserIdAndRefId(userId: String, refId: String): PointTransaction?

    fun countByUserIdAndRuleCode(userId: String, ruleCode: String): Long

    fun countByUserIdAndRuleCodeAndCreatedDateGreaterThanEqual(userId: String, ruleCode: String, since: LocalDateTime): Long
}
