package com.example.pointservice.application.usecase

import com.example.pointservice.application.service.PointRuleCommandService
import com.example.pointservice.application.service.PointRuleQueryService
import com.example.pointservice.application.usecase.command.CreatePointRuleCommand
import com.example.pointservice.application.usecase.command.UpdatePointRuleCommand
import com.example.pointservice.application.usecase.result.PointRuleResult
import org.springframework.stereotype.Component

/** 적립 규칙 관리(백오피스). */
@Component
class PointRuleUseCase(
    private val pointRuleQueryService: PointRuleQueryService,
    private val pointRuleCommandService: PointRuleCommandService,
) {

    /** 모든 규칙(비활성 포함), 코드 순. */
    fun rules(): List<PointRuleResult> = pointRuleQueryService.rules()

    fun create(command: CreatePointRuleCommand): PointRuleResult = pointRuleCommandService.create(command)

    fun update(code: String, command: UpdatePointRuleCommand): PointRuleResult = pointRuleCommandService.update(code, command)

    fun delete(code: String) = pointRuleCommandService.delete(code)
}
