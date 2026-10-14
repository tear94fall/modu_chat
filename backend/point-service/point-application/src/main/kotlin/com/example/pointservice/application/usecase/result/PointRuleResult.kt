package com.example.pointservice.application.usecase.result

import com.example.pointservice.application.domain.entity.PointRule

data class PointRuleResult(
    val code: String,
    val name: String,
    val points: Long,
    val dailyLimit: Int?,
    val totalLimit: Int?,
    val enabled: Boolean,
) {
    companion object {
        fun from(r: PointRule) = PointRuleResult(r.code, r.name, r.points, r.dailyLimit, r.totalLimit, r.enabled)
    }
}
