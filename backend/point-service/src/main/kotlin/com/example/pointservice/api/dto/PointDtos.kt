package com.example.pointservice.api.dto

import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.entity.PointAccount
import com.example.pointservice.point.entity.PointRule
import com.example.pointservice.point.entity.PointTransaction
import com.example.pointservice.point.entity.PointTransactionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
    /** 금액 지정 적립의 출처(PURCHASE 등). 그 밖의 줄은 null. */
    val reason: String?,
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
            reason = t.reason,
            createdDate = t.createdDate,
        )
    }
}

/** 다른 서비스가 적립을 요청할 때. 점수는 규칙이 정하므로 코드만 보낸다. */
@Schema(description = "규칙 코드로 적립 요청. 점수·상한은 규칙이 정한다.")
data class EarnRequestDto(
    @field:Schema(description = "적립받을 회원 userId. 필수", example = "11")
    @field:NotBlank val userId: String = "",
    @field:Schema(description = "적립 규칙 코드. 필수, 없는 코드면 404", example = "INVITE_FRIEND")
    @field:NotBlank val ruleCode: String = "",
    /** 멱등 키. 같은 사용자·같은 refId 는 한 번만 적립된다(초대 id, 주문 번호 등). */
    @field:Schema(description = "멱등 키(선택, 최대 128자). 같은 회원·같은 refId 는 한 번만 적립된다", example = "invite:42")
    @field:Size(max = 128) val refId: String? = null,
    @field:Schema(description = "원장 메모(선택, 최대 200자). 비우면 규칙 이름이 남는다", example = "친구 초대 보상")
    @field:Size(max = 200) val memo: String? = null,
)

/** 규칙 없이 금액을 정해 적립할 때(구매 적립 등). 규칙이 없으니 상한도 없고, 멱등 키는 필수다. */
@Schema(description = "금액 지정 적립 요청(구매 적립 등). 규칙·상한이 없고 refId 가 필수다.")
data class EarnAmountRequestDto(
    @field:Schema(description = "적립받을 회원 userId. 필수", example = "11")
    @field:NotBlank val userId: String = "",
    @field:Schema(description = "적립 금액. 1~1,000,000", example = "120")
    @field:NotNull @field:Min(1) @field:Max(1_000_000) val amount: Long = 0L,
    /** 적립 출처 코드(PURCHASE 등). 원장의 reason 칸에 남아 관리자·사용자 화면이 출처를 보여 준다. */
    @field:Schema(description = "적립 출처 코드. 필수, 최대 30자. 원장 reason 칸에 남는다", example = "PURCHASE")
    @field:NotBlank @field:Size(max = 30) val reason: String = "",
    @field:Schema(description = "멱등 키. 필수, 최대 128자. 같은 회원·같은 refId 는 한 번만 적립된다", example = "purchase:order:1024")
    @field:NotBlank @field:Size(max = 128) val refId: String = "",
    @field:Schema(description = "원장 메모(선택, 최대 200자)", example = "구매 적립")
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

@Schema(description = "포인트 사용(차감) 또는 사용 취소(환불) 요청.")
data class SpendRequestDto(
    @field:Schema(description = "회원 userId. 필수", example = "11")
    @field:NotBlank val userId: String = "",
    @field:Schema(description = "사용·환불 금액. 1 이상(0 이하면 400)", example = "500")
    @field:NotNull val amount: Long = 0L,
    /** 필수 멱등 키(주문 번호 등). 같은 키로 다시 오면 차감하지 않고 현재 잔액을 돌려준다. */
    @field:Schema(
        description = "멱등 키. 필수, 최대 128자. 원장 전체에서 회원별로 한 번만 쓰이므로 사용과 환불은 다른 키를 쓴다",
        example = "order:1024",
    )
    @field:NotBlank @field:Size(max = 128) val refId: String = "",
    @field:Schema(description = "원장 메모(선택, 최대 200자)", example = "주문 결제")
    @field:Size(max = 200) val memo: String? = null,
)

/** 사용·환불 결과. 같은 refId 가 두 번 오면 [applied] 가 false 다. */
data class SpendResultDto(val applied: Boolean, val amount: Long, val balance: Long)

/** 관리자 수동 조정. 양수는 지급, 음수는 회수. */
@Schema(description = "관리자 수동 조정. 양수는 지급, 음수는 회수.")
data class AdjustRequestDto(
    @field:Schema(description = "조정 금액. 양수는 지급, 음수는 회수, 0 이면 400. 회수는 잔액을 넘을 수 없다", example = "-100")
    @field:NotNull val amount: Long = 0L,
    @field:Schema(description = "조정 사유. 필수, 최대 200자. 원장에 남는다", example = "CS 보상")
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
@Schema(description = "새 적립 규칙.")
data class PointRuleCreateDto(
    @field:Schema(description = "규칙 코드. 필수, 대문자로 시작하는 대문자·숫자·밑줄 2~32자. 다른 서비스가 그대로 보낸다", example = "REVIEW_WRITE")
    @field:NotBlank @field:Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "대문자로 시작하는 대문자·숫자·밑줄 2~32자") val code: String = "",
    @field:Schema(description = "규칙 이름. 필수, 최대 100자. 메모가 없을 때 원장에 남는다", example = "리뷰 작성")
    @field:NotBlank @field:Size(max = 100) val name: String = "",
    @field:Schema(description = "한 번 적립할 점수. 필수", example = "50")
    @field:NotNull val points: Long = 0L,
    @field:Schema(description = "회원별 하루(한국 시간) 적립 횟수 상한. null 이면 무제한", example = "3")
    val dailyLimit: Int? = null,
    @field:Schema(description = "회원별 전체 적립 횟수 상한. null 이면 무제한", example = "100")
    val totalLimit: Int? = null,
    @field:Schema(description = "활성 여부(기본 true). 비활성이면 적립 요청이 applied=false 로 답한다", example = "true")
    val enabled: Boolean = true,
)

@Schema(description = "적립 규칙 수정. 코드를 뺀 모든 값을 통째로 바꾼다.")
data class PointRuleUpdateDto(
    @field:Schema(description = "규칙 이름. 필수, 최대 100자", example = "출석 체크")
    @field:NotBlank @field:Size(max = 100) val name: String = "",
    @field:Schema(description = "한 번 적립할 점수. 필수", example = "10")
    @field:NotNull val points: Long = 0L,
    @field:Schema(description = "회원별 하루(한국 시간) 적립 횟수 상한. null 이면 무제한", example = "1")
    val dailyLimit: Int? = null,
    @field:Schema(description = "회원별 전체 적립 횟수 상한. null 이면 무제한")
    val totalLimit: Int? = null,
    @field:Schema(description = "활성 여부(기본 true). 비활성이면 적립 요청이 applied=false 로 답한다", example = "true")
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
