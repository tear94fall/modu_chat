package com.example.pointservice.application.usecase.command

/** 규칙 코드로 적립. 점수·상한은 규칙이 정한다. [refId] 는 멱등 키(선택). */
data class EarnCommand(
    val userId: String,
    val ruleCode: String,
    val refId: String? = null,
    val memo: String? = null,
)

/** 규칙 없이 금액을 정해 적립(구매 적립 등). [reason] 은 원장에 남는 출처, [refId] 는 필수 멱등 키. */
data class EarnAmountCommand(
    val userId: String,
    val amount: Long,
    val reason: String,
    val refId: String,
    val memo: String? = null,
)

/** 사용(차감)과 사용 취소(환불)가 같이 쓴다. [amount] 는 양수, [refId] 는 필수 멱등 키. */
data class SpendCommand(
    val userId: String,
    val amount: Long,
    val refId: String,
    val memo: String? = null,
)

/** 관리자 수동 조정. 양수는 지급, 음수는 회수. */
data class AdjustCommand(
    val userId: String,
    val amount: Long,
    val memo: String,
)
