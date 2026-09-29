package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.CommonData
import java.util.Optional

/** 앱이 켜질 때 읽는 공통 설정(replica). */
interface CommonDataRoRepository : RoRepository<CommonData, String> {

    fun findById(key: String): Optional<CommonData>
}
