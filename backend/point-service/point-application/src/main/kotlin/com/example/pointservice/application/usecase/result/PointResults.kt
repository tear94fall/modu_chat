package com.example.pointservice.application.usecase.result

import com.example.pointservice.application.domain.entity.PointAccount
import com.example.pointservice.application.domain.entity.PointTransaction
import com.example.pointservice.application.domain.entity.PointTransactionType
import com.example.pointservice.application.member.MemberSummary
import java.time.LocalDateTime

data class PointBalanceResult(val userId: String, val balance: Long)

data class PointTransactionResult(
    val id: Long,
    val type: PointTransactionType,
    val amount: Long,
    val balanceAfter: Long,
    val ruleCode: String?,
    val refId: String?,
    val memo: String?,
    /** 금액 지정 적립의 출처(PURCHASE 등). 그 밖의 줄은 null. */
    val reason: String?,
    val createdDate: LocalDateTime?,
) {
    companion object {
        fun from(t: PointTransaction) = PointTransactionResult(
            id = requireNotNull(t.id),
            type = t.type,
            amount = t.amount,
            balanceAfter = t.balanceAfter,
            ruleCode = t.ruleCode,
            refId = t.refId,
            memo = t.memo,
            reason = t.reason,
            createdDate = t.createdDate,
        )
    }
}

/** 적립 결과. 상한·중복·비활성 규칙이면 [applied] 가 false 이고 [reason] 에 이유가 있다(오류가 아니다). */
data class EarnResult(
    val applied: Boolean,
    val amount: Long,
    val balance: Long,
    val reason: EarnSkipReason? = null,
)

enum class EarnSkipReason { DUPLICATE, RULE_DISABLED, DAILY_LIMIT, TOTAL_LIMIT }

/** 사용·환불 결과. 같은 refId 가 두 번 오면 [applied] 가 false 다. */
data class SpendResult(val applied: Boolean, val amount: Long, val balance: Long)

/** 포인트 계정 한 줄(회원 정보 없이). */
data class PointAccountResult(
    val userId: String,
    val balance: Long,
    val createdDate: LocalDateTime?,
    val updatedDate: LocalDateTime?,
) {
    companion object {
        fun from(a: PointAccount) = PointAccountResult(a.userId, a.balance, a.createdDate, a.updatedDate)
    }
}

/** 백오피스 계정 한 줄. 이름·이메일은 member-service 에서 붙인 것이라 조회가 안 되면 null 이다. */
data class AdminPointAccountResult(
    val userId: String,
    val username: String?,
    val email: String?,
    val balance: Long,
    val createdDate: LocalDateTime?,
    val updatedDate: LocalDateTime?,
) {
    companion object {
        fun of(account: PointAccountResult, member: MemberSummary?) = AdminPointAccountResult(
            userId = account.userId,
            username = member?.username,
            email = member?.email,
            balance = account.balance,
            createdDate = account.createdDate,
            updatedDate = account.updatedDate,
        )
    }
}
