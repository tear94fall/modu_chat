package com.example.memberservice.application.usecase

import com.example.memberservice.application.service.CommonDataCommandService
import com.example.memberservice.application.service.CommonDataQueryService
import com.example.memberservice.application.usecase.result.CommonDataResult
import org.springframework.stereotype.Component

/** 공통 설정(key/value). 앱은 레플리카에서 읽고, 백오피스는 저장 직후에 다시 읽으므로 master 에서 읽는다. */
@Component
class CommonDataUseCase(
    private val commonDataQueryService: CommonDataQueryService,
    private val commonDataCommandService: CommonDataCommandService,
) {

    /** 앱용. 없는 키면 null, 형식이 틀리면 400. */
    fun get(key: String?): CommonDataResult? = commonDataQueryService.get(key)

    /** 백오피스용. 없는 키면 null, 형식이 틀리면 400. */
    fun getForAdmin(key: String?): CommonDataResult? = commonDataCommandService.get(key)

    fun getAllForAdmin(): List<CommonDataResult> = commonDataCommandService.getAll()

    fun upsert(key: String?, value: String): CommonDataResult = commonDataCommandService.upsert(key, value)
}
