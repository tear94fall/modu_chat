package com.example.pointservice.application.service

import com.example.pointservice.application.common.exception.CustomException
import com.example.pointservice.application.common.exception.ErrorCode
import com.example.pointservice.application.config.RwJpaConfig
import com.example.pointservice.application.domain.entity.PointAccount
import com.example.pointservice.application.domain.entity.PointTransaction
import com.example.pointservice.application.domain.entity.PointTransactionType
import com.example.pointservice.application.domain.repository.rw.PointAccountRwRepository
import com.example.pointservice.application.domain.repository.rw.PointRuleRwRepository
import com.example.pointservice.application.domain.repository.rw.PointTransactionRwRepository
import com.example.pointservice.application.usecase.command.AdjustCommand
import com.example.pointservice.application.usecase.command.CancelSpendCommand
import com.example.pointservice.application.usecase.command.EarnAmountCommand
import com.example.pointservice.application.usecase.command.EarnCommand
import com.example.pointservice.application.usecase.command.SpendCommand
import com.example.pointservice.application.usecase.result.EarnResult
import com.example.pointservice.application.usecase.result.EarnSkipReason
import com.example.pointservice.application.usecase.result.PointAccountResult
import com.example.pointservice.application.usecase.result.PointBalanceResult
import com.example.pointservice.application.usecase.result.PointTransactionResult
import com.example.pointservice.application.usecase.result.SpendCancelResult
import com.example.pointservice.application.usecase.result.SpendCancelSkipReason
import com.example.pointservice.application.usecase.result.SpendResult
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 원장 쓰기(master). 잔액 변경은 계정 행을 잠근 트랜잭션 안에서만 하고, 원장에 한 줄씩 남긴다.
 * 적립은 규칙 코드로만 받는다. 점수·상한은 규칙이 정하고, 중복(refId)·상한 초과는 오류가 아니라 "적용 안 됨" 으로 답한다
 * — 호출 쪽(다른 서비스)이 재시도해도 안전하고, 상한에 걸렸다고 그쪽 흐름이 실패하면 안 되기 때문이다.
 *
 * 쓰기 전에 보는 검사(규칙·멱등 키·상한·잔액)는 모두 master 에서 읽는다. 레플리카는 늦을 수 있어
 * 거기서 읽으면 같은 refId 가 두 번 적립되거나 상한을 넘는다.
 *
 * "쓰기 직후 읽기" 도 여기 있다([balance], [history], [account]). 읽기 전용이지만 master 로 읽는다 —
 * 앱·커머스·백오피스가 적립·사용·조정 바로 뒤에 부르기 때문이다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class PointCommandService(
    private val accountRwRepository: PointAccountRwRepository,
    private val transactionRwRepository: PointTransactionRwRepository,
    private val ruleRwRepository: PointRuleRwRepository,
    private val clock: Clock,
) {

    /** 현재 잔액. 계정이 없으면 0. 결제 화면·마이 페이지가 적립·사용 직후에 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun balance(userId: String): PointBalanceResult =
        PointBalanceResult(userId, accountRwRepository.findByUserId(userId).map { it.balance }.orElse(0L))

    /** 한 회원의 원장(최근 순). 적립·사용·조정 직후에 다시 읽는 화면이 방금 줄을 봐야 한다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun history(userId: String, pageable: Pageable): Page<PointTransactionResult> =
        transactionRwRepository.findByUserIdOrderByIdDesc(userId, pageable).map { PointTransactionResult.from(it) }

    /** 한 회원의 계정. 백오피스 상세가 조정 직후 다시 읽는다(첫 조정이면 계정도 방금 생겼다). */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun account(userId: String): PointAccountResult =
        accountRwRepository.findByUserId(userId)
            .map { PointAccountResult.from(it) }
            .orElseThrow { CustomException(ErrorCode.ACCOUNT_NOT_FOUND, userId) }

    fun earn(command: EarnCommand): EarnResult {
        val (userId, ruleCode, refId, memo) = command
        val rule = ruleRwRepository.findById(ruleCode).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, ruleCode) }
        val account = lockOrCreate(userId)
        if (!rule.enabled) return EarnResult(false, 0L, account.balance, EarnSkipReason.RULE_DISABLED)
        if (refId != null && transactionRwRepository.existsByUserIdAndRefId(userId, refId)) {
            return EarnResult(false, 0L, account.balance, EarnSkipReason.DUPLICATE)
        }
        rule.totalLimit?.let { limit ->
            if (transactionRwRepository.countByUserIdAndRuleCode(userId, rule.code) >= limit) {
                return EarnResult(false, 0L, account.balance, EarnSkipReason.TOTAL_LIMIT)
            }
        }
        rule.dailyLimit?.let { limit ->
            val startOfDay = LocalDate.now(clock).atStartOfDay()
            if (transactionRwRepository.countByUserIdAndRuleCodeAndCreatedDateGreaterThanEqual(userId, rule.code, startOfDay) >= limit) {
                return EarnResult(false, 0L, account.balance, EarnSkipReason.DAILY_LIMIT)
            }
        }
        val balance = account.apply(rule.points)
        transactionRwRepository.save(
            PointTransaction(userId, PointTransactionType.EARN, rule.points, balance, now(), ruleCode = rule.code, refId = refId, memo = memo ?: rule.name),
        )
        return EarnResult(true, rule.points, balance)
    }

    /**
     * 금액 지정 적립(구매 적립 등). 규칙·상한 없이 금액을 그대로 적립하고 원장에 출처(reason)를 남긴다.
     * 규칙 적립과 같이 (userId, refId) 로 멱등이다 — 다시 오면 applied=false, DUPLICATE.
     */
    fun earnAmount(command: EarnAmountCommand): EarnResult {
        val (userId, amount, reason, refId, memo) = command
        if (amount <= 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, amount.toString())
        val account = lockOrCreate(userId)
        if (transactionRwRepository.existsByUserIdAndRefId(userId, refId)) {
            return EarnResult(false, 0L, account.balance, EarnSkipReason.DUPLICATE)
        }
        val balance = account.apply(amount)
        transactionRwRepository.save(
            PointTransaction(userId, PointTransactionType.EARN, amount, balance, now(), refId = refId, memo = memo, reason = reason),
        )
        return EarnResult(true, amount, balance)
    }

    /** 출석 체크. 하루 한 번은 규칙(DAILY_CHECKIN 의 dailyLimit)이 막고, refId 로도 한 번 더 막는다. 하루는 한국 시간 기준. */
    fun checkIn(userId: String): EarnResult =
        earn(EarnCommand(userId, CHECKIN_RULE, refId = "checkin:${LocalDate.now(clock)}"))

    fun spend(command: SpendCommand): SpendResult {
        val (userId, amount, refId, memo) = command
        if (amount <= 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, amount.toString())
        val account = lockOrCreate(userId)
        if (transactionRwRepository.existsByUserIdAndRefId(userId, refId)) {
            return SpendResult(false, 0L, account.balance)
        }
        if (account.balance < amount) throw CustomException(ErrorCode.INSUFFICIENT_POINT, "잔액 ${account.balance}, 요청 $amount")
        val balance = account.apply(-amount)
        transactionRwRepository.save(PointTransaction(userId, PointTransactionType.SPEND, -amount, balance, now(), refId = refId, memo = memo))
        return SpendResult(true, amount, balance)
    }

    /** 사용 취소(환불). 주문 취소처럼 앞서 차감한 포인트를 돌려준다. 같은 refId 로 다시 오면 돌려주지 않고 현재 잔액만 준다. */
    fun refund(command: SpendCommand): SpendResult {
        val (userId, amount, refId, memo) = command
        if (amount <= 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, amount.toString())
        val account = lockOrCreate(userId)
        if (transactionRwRepository.existsByUserIdAndRefId(userId, refId)) {
            return SpendResult(false, 0L, account.balance)
        }
        val balance = account.apply(amount)
        transactionRwRepository.save(PointTransaction(userId, PointTransactionType.REFUND, amount, balance, now(), refId = refId, memo = memo))
        return SpendResult(true, amount, balance)
    }

    /**
     * 사용 취소: "사용했으면 되돌린다". 원래 사용(SPEND, 같은 userId·refId) 이 있을 때만 그 금액을 REFUND 로 돌려주고,
     * 환불 줄의 refId 는 "refund:" + refId 다 — 커머스 주문 취소 환불(refund:order:…)과 같은 키라 어느 쪽으로 와도 한 번만 돌려준다.
     * 계정 행을 잠근 뒤 확인하고 쓰므로 동시에 두 번 와도 한 번만 돌려준다. 계정이 없으면 만들지 않는다(사용도 없었다).
     */
    fun cancelSpend(command: CancelSpendCommand): SpendCancelResult {
        val (userId, refId, memo) = command
        val account = accountRwRepository.findByUserIdForUpdate(userId).orElse(null)
            ?: return SpendCancelResult(false, SpendCancelSkipReason.NO_SPEND, 0L, 0L)
        val spend = transactionRwRepository.findByUserIdAndRefId(userId, refId)
        if (spend == null || spend.type != PointTransactionType.SPEND) {
            return SpendCancelResult(false, SpendCancelSkipReason.NO_SPEND, 0L, account.balance)
        }
        val refundRefId = refundRefIdOf(refId)
        if (transactionRwRepository.existsByUserIdAndRefId(userId, refundRefId)) {
            return SpendCancelResult(false, SpendCancelSkipReason.ALREADY_REFUNDED, 0L, account.balance)
        }
        // 사용 줄의 amount 는 음수(−차감액)로 적혀 있다.
        val amount = Math.abs(spend.amount)
        val balance = account.apply(amount)
        transactionRwRepository.saveAndFlush(
            PointTransaction(userId, PointTransactionType.REFUND, amount, balance, now(), refId = refundRefId, memo = memo ?: "사용 취소"),
        )
        return SpendCancelResult(true, null, amount, balance)
    }

    /** 관리자 수동 조정. 회수는 잔액을 넘을 수 없다. */
    fun adjust(command: AdjustCommand): PointBalanceResult {
        val (userId, amount, memo) = command
        if (amount == 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, "0")
        val account = lockOrCreate(userId)
        if (account.balance + amount < 0L) {
            throw CustomException(ErrorCode.INSUFFICIENT_POINT, "잔액 ${account.balance}, 조정 $amount")
        }
        val balance = account.apply(amount)
        transactionRwRepository.save(PointTransaction(userId, PointTransactionType.ADJUST, amount, balance, now(), memo = memo))
        return PointBalanceResult(userId, balance)
    }

    /** 잠근 계정을 돌려준다. 없으면 만들고 잠근다(첫 적립). */
    private fun lockOrCreate(userId: String): PointAccount =
        accountRwRepository.findByUserIdForUpdate(userId).orElseGet {
            accountRwRepository.saveAndFlush(PointAccount(userId))
            accountRwRepository.findByUserIdForUpdate(userId).orElseThrow()
        }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    companion object {
        const val CHECKIN_RULE = "DAILY_CHECKIN"

        /** 사용 취소·주문 취소 환불이 같이 쓰는 환불 줄의 refId 접두사. */
        const val REFUND_REF_PREFIX = "refund:"

        fun refundRefIdOf(spendRefId: String): String = REFUND_REF_PREFIX + spendRefId
    }
}
