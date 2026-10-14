package com.example.pointservice.application.service

import com.example.pointservice.application.common.exception.CustomException
import com.example.pointservice.application.common.exception.ErrorCode
import com.example.pointservice.application.config.RwJpaConfig
import com.example.pointservice.application.domain.entity.PointRule
import com.example.pointservice.application.domain.repository.rw.PointRuleRwRepository
import com.example.pointservice.application.usecase.command.CreatePointRuleCommand
import com.example.pointservice.application.usecase.command.UpdatePointRuleCommand
import com.example.pointservice.application.usecase.result.PointRuleResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 적립 규칙 쓰기(master). 바뀐 값은 다음 적립부터 적용된다(적립은 규칙을 master 에서 읽는다). */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class PointRuleCommandService(private val ruleRwRepository: PointRuleRwRepository) {

    fun create(command: CreatePointRuleCommand): PointRuleResult {
        if (ruleRwRepository.existsById(command.code)) throw CustomException(ErrorCode.RULE_ALREADY_EXISTS, command.code)
        val rule = ruleRwRepository.save(
            PointRule(command.code, command.name, command.points, command.dailyLimit, command.totalLimit, command.enabled),
        )
        return PointRuleResult.from(rule)
    }

    fun update(code: String, command: UpdatePointRuleCommand): PointRuleResult {
        val rule = ruleRwRepository.findById(code).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, code) }
        rule.update(command.name, command.points, command.dailyLimit, command.totalLimit, command.enabled)
        return PointRuleResult.from(rule)
    }

    /** 원장은 규칙 코드를 문자열로 들고 있어 규칙을 지워도 과거 이력은 남는다. 출석 규칙만은 코드가 고정이라 지우지 못한다. */
    fun delete(code: String) {
        if (code == PointCommandService.CHECKIN_RULE) throw CustomException(ErrorCode.RULE_IN_USE, code)
        val rule = ruleRwRepository.findById(code).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, code) }
        ruleRwRepository.delete(rule)
    }
}
