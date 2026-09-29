package com.example.pointservice.application.domain.repository.ro

import com.example.pointservice.application.config.RoRepository
import com.example.pointservice.application.domain.entity.PointAccount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PointAccountRoRepository : RoRepository<PointAccount, Long> {

    fun findAll(pageable: Pageable): Page<PointAccount>

    fun findByUserIdIn(userIds: Collection<String>, pageable: Pageable): Page<PointAccount>
}
