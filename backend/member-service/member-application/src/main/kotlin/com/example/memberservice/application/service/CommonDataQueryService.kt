package com.example.memberservice.application.service

import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.repository.ro.CommonDataRoRepository
import com.example.memberservice.application.usecase.result.CommonDataResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 앱이 켜질 때 읽는 공통 설정(replica). */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class CommonDataQueryService(private val commonDataRoRepository: CommonDataRoRepository) {

    /** 없는 키면 null. */
    fun get(key: String?): CommonDataResult? =
        commonDataRoRepository.findById(CommonDataKeys.validate(key)).map(CommonDataResult::from).orElse(null)
}
