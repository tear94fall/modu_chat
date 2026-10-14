package com.example.pointservice.application.service

import com.example.pointservice.application.config.RoJpaConfig
import com.example.pointservice.application.domain.repository.ro.PointRuleRoRepository
import com.example.pointservice.application.usecase.result.PointRuleResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 적립 규칙 목록(replica). 백오피스는 추가·수정·삭제 응답으로 화면을 고치므로 목록은 조금 늦어도 된다. */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class PointRuleQueryService(private val ruleRoRepository: PointRuleRoRepository) {

    fun rules(): List<PointRuleResult> = ruleRoRepository.findAllByOrderByCodeAsc().map { PointRuleResult.from(it) }
}
