package com.example.pointservice.application.domain.repository.rw

import com.example.pointservice.application.config.RwRepository
import com.example.pointservice.application.domain.entity.PointTransaction
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PointTransactionRwRepository : RwRepository<PointTransaction, Long> {

    fun findByUserIdOrderByIdDesc(userId: String, pageable: Pageable): Page<PointTransaction>

    fun existsByUserIdAndRefId(userId: String, refId: String): Boolean

    fun countByUserIdAndRuleCode(userId: String, ruleCode: String): Long

    fun countByUserIdAndRuleCodeAndCreatedDateGreaterThanEqual(userId: String, ruleCode: String, since: LocalDateTime): Long
}
