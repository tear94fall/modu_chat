package com.example.pointservice.point.repository

import com.example.pointservice.point.entity.PointRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointRuleRepository : JpaRepository<PointRule, String>
