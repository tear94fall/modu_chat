package com.example.pointservice.point.repository

import com.example.pointservice.point.entity.PointTransaction
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointTransactionRepository : JpaRepository<PointTransaction, Long> {

    fun findByUserIdOrderByIdDesc(userId: String, pageable: Pageable): Page<PointTransaction>

    fun existsByUserIdAndRefId(userId: String, refId: String): Boolean

    fun countByUserIdAndRuleCode(userId: String, ruleCode: String): Long

    fun countByUserIdAndRuleCodeAndCreatedDateGreaterThanEqual(userId: String, ruleCode: String, since: LocalDateTime): Long
}
