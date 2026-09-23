package com.example.pointservice.api.dto

import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.entity.PointAccount
import com.example.pointservice.point.entity.PointRule
import com.example.pointservice.point.entity.PointTransaction
import com.example.pointservice.point.entity.PointTransactionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class PointBalanceDto(val userId: String, val balance: Long)

data class PointTransactionDto(
    val id: Long,
    val type: PointTransactionType,
    val amount: Long,
    val balanceAfter: Long,
    val ruleCode: String?,
    val refId: String?,
    val memo: String?,
    val createdDate: LocalDateTime?,
) {
    companion object {
        fun of(t: PointTransaction) = PointTransactionDto(
            id = t.id!!,
            type = t.type,
            amount = t.amount,
            balanceAfter = t.balanceAfter,
            ruleCode = t.ruleCode,
            refId = t.refId,
            memo = t.memo,
            createdDate = t.createdDate,
        )
    }
}

/** 다른 서비스가 적립을 요청할 때. 점수는 규칙이 정하므로 코드만 보낸다. */
data class EarnRequestDto(
    @field:NotBlank val userId: String = "",
    @field:NotBlank val ruleCode: String = "",
    /** 멱등 키. 같은 사용자·같은 refId 는 한 번만 적립된다(초대 id, 주문 번호 등). */
    @field:Size(max = 128) val refId: String? = null,
    @field:Size(max = 200) val memo: String? = null,
)

/** 적립 결과. 상한·중복·비활성 규칙이면 [applied] 가 false 이고 [reason] 에 이유가 있다(오류가 아니다). */
data class EarnResultDto(
    val applied: Boolean,
    val amount: Long,
    val balance: Long,
    val reason: EarnSkipReason? = null,
)

enum class EarnSkipReason { DUPLICATE, RULE_DISABLED, DAILY_LIMIT, TOTAL_LIMIT }

data class SpendRequestDto(
    @field:NotBlank val userId: String = "",
    @field:NotNull val amount: Long = 0L,
    /** 필수 멱등 키(주문 번호 등). 같은 키로 다시 오면 차감하지 않고 현재 잔액을 돌려준다. */
    @field:NotBlank @field:Size(max = 128) val refId: String = "",
    @field:Size(max = 200) val memo: String? = null,
)

/** 사용·환불 결과. 같은 refId 가 두 번 오면 [applied] 가 false 다. */
data class SpendResultDto(val applied: Boolean, val amount: Long, val balance: Long)

/** 관리자 수동 조정. 양수는 지급, 음수는 회수. */
data class AdjustRequestDto(
    @field:NotNull val amount: Long = 0L,
    @field:NotBlank @field:Size(max = 200) val memo: String = "",
)

data class PointRuleDto(
    val code: String,
    val name: String,
    val points: Long,
    val dailyLimit: Int?,
    val totalLimit: Int?,
    val enabled: Boolean,
) {
    companion object {
        fun of(r: PointRule) = PointRuleDto(r.code, r.name, r.points, r.dailyLimit, r.totalLimit, r.enabled)
    }
}

/** 새 규칙. 코드는 대문자·숫자·밑줄만(다른 서비스가 그대로 보내는 식별자). */
data class PointRuleCreateDto(
    @field:NotBlank @field:Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "대문자로 시작하는 대문자·숫자·밑줄 2~32자") val code: String = "",
    @field:NotBlank @field:Size(max = 100) val name: String = "",
    @field:NotNull val points: Long = 0L,
    val dailyLimit: Int? = null,
    val totalLimit: Int? = null,
    val enabled: Boolean = true,
)

data class PointRuleUpdateDto(
    @field:NotBlank @field:Size(max = 100) val name: String = "",
    @field:NotNull val points: Long = 0L,
    val dailyLimit: Int? = null,
    val totalLimit: Int? = null,
    val enabled: Boolean = true,
)

/** 백오피스 계정 한 줄. 이름·이메일은 member-service 에서 붙인 것이라 조회가 안 되면 null 이다. */
data class AdminPointAccountDto(
    val userId: String,
    val username: String?,
    val email: String?,
    val balance: Long,
    val createdDate: LocalDateTime?,
    val updatedDate: LocalDateTime?,
) {
    companion object {
        fun of(a: PointAccount, member: MemberSummaryDto?) =
            AdminPointAccountDto(a.userId, member?.username, member?.email, a.balance, a.createdDate, a.updatedDate)
    }
}
