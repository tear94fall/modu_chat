package com.example.pointservice.application.usecase.command

data class CreatePointRuleCommand(
    val code: String,
    val name: String,
    val points: Long,
    val dailyLimit: Int? = null,
    val totalLimit: Int? = null,
    val enabled: Boolean = true,
)

/** 코드를 뺀 모든 값을 통째로 바꾼다. */
data class UpdatePointRuleCommand(
    val name: String,
    val points: Long,
    val dailyLimit: Int? = null,
    val totalLimit: Int? = null,
    val enabled: Boolean = true,
)
