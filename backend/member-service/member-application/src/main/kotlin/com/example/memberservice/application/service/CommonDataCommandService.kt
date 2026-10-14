package com.example.memberservice.application.service

import com.example.memberservice.application.config.RwJpaConfig
import com.example.memberservice.application.domain.entity.CommonData
import com.example.memberservice.application.domain.repository.rw.CommonDataRwRepository
import com.example.memberservice.application.usecase.result.CommonDataResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 공통 설정 쓰기(master)와, 백오피스가 저장 직후 다시 읽는 조회. */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class CommonDataCommandService(private val commonDataRwRepository: CommonDataRwRepository) {

    /** 없는 키면 null. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun get(key: String?): CommonDataResult? =
        commonDataRwRepository.findById(CommonDataKeys.validate(key)).map(CommonDataResult::from).orElse(null)

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getAll(): List<CommonDataResult> =
        commonDataRwRepository.findAll().sortedBy { it.key }.map(CommonDataResult::from)

    /** 있으면 값만 갈아 끼우고 없으면 새로 만든다. 백오피스에서 키를 따로 만들 화면을 두지 않으려는 것이다. */
    fun upsert(key: String?, value: String): CommonDataResult {
        val validKey = CommonDataKeys.validate(key)
        val commonData = commonDataRwRepository.findById(validKey).orElse(null)
            ?: return CommonDataResult.from(commonDataRwRepository.save(CommonData(validKey, value)))
        commonData.updateValue(value)
        return CommonDataResult.from(commonData)
    }
}
