package com.example.pushservice.application.domain.repository.ro

import com.example.pushservice.application.config.RoRepository
import com.example.pushservice.application.domain.entity.FcmToken
import java.util.Optional

interface FcmTokenRoRepository : RoRepository<FcmToken, Long> {

    /** userId 당 여러 행이 남아있을 수 있어(과거 버그) 가장 최근 것 하나만 가져온다. */
    fun findFirstByUserIdOrderByIdDesc(userId: String?): Optional<FcmToken>

    fun findAll(): List<FcmToken>
}
