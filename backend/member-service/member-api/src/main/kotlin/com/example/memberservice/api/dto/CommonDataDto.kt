package com.example.memberservice.api.dto

import com.example.memberservice.application.usecase.result.CommonDataResult

/** 안드로이드가 기대하는 모양 그대로다: {"key":"version","value":"1.2.3"}. */
data class CommonDataDto(val key: String, val value: String) {
    companion object {
        fun of(result: CommonDataResult): CommonDataDto = CommonDataDto(result.key, result.value)
    }
}
