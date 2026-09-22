package com.example.memberservice.common.dto

import com.example.memberservice.common.entity.CommonData

/** 안드로이드가 기대하는 모양 그대로다: {"key":"version","value":"1.2.3"}. */
data class CommonDataDto(val key: String, val value: String) {
    companion object {
        fun from(commonData: CommonData): CommonDataDto = CommonDataDto(commonData.key, commonData.value)
    }
}
