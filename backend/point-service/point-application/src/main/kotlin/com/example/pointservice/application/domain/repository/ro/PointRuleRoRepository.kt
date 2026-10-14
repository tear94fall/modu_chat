package com.example.pointservice.application.domain.repository.ro

import com.example.pointservice.application.config.RoRepository
import com.example.pointservice.application.domain.entity.PointRule

interface PointRuleRoRepository : RoRepository<PointRule, String> {

    fun findAllByOrderByCodeAsc(): List<PointRule>
}
