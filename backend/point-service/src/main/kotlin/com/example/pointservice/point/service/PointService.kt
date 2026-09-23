package com.example.pointservice.point.service

import com.example.pointservice.api.dto.AdjustRequestDto
import com.example.pointservice.api.dto.AdminPointAccountDto
import com.example.pointservice.api.dto.EarnResultDto
import com.example.pointservice.api.dto.EarnSkipReason
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointRuleCreateDto
import com.example.pointservice.api.dto.PointRuleDto
import com.example.pointservice.api.dto.PointRuleUpdateDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.api.dto.SpendResultDto
import com.example.pointservice.common.exception.CustomException
import com.example.pointservice.common.exception.ErrorCode
import com.example.pointservice.member.MemberLookup
import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.entity.PointAccount
import com.example.pointservice.point.entity.PointRule
import com.example.pointservice.point.entity.PointTransaction
import com.example.pointservice.point.entity.PointTransactionType
import com.example.pointservice.point.repository.PointAccountRepository
import com.example.pointservice.point.repository.PointRuleRepository
import com.example.pointservice.point.repository.PointTransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 적립·사용·조정. 잔액 변경은 계정 행을 잠근 트랜잭션 안에서만 하고, 원장에 한 줄씩 남긴다.
 * 적립은 규칙 코드로만 받는다. 점수·상한은 규칙이 정하고, 중복(refId)·상한 초과는 오류가 아니라 "적용 안 됨" 으로 답한다
 * — 호출 쪽(다른 서비스)이 재시도해도 안전하고, 상한에 걸렸다고 그쪽 흐름이 실패하면 안 되기 때문이다.
 */
@Service
@Transactional
class PointService(
    private val accountRepository: PointAccountRepository,
    private val transactionRepository: PointTransactionRepository,
    private val ruleRepository: PointRuleRepository,
    private val memberLookup: MemberLookup,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun balance(userId: String): PointBalanceDto =
        PointBalanceDto(userId, accountRepository.findByUserId(userId).map { it.balance }.orElse(0L))

    @Transactional(readOnly = true)
    fun history(userId: String, pageable: Pageable): Page<PointTransactionDto> =
        transactionRepository.findByUserIdOrderByIdDesc(userId, pageable).map { PointTransactionDto.of(it) }

    fun earn(userId: String, ruleCode: String, refId: String? = null, memo: String? = null): EarnResultDto {
        val rule = ruleRepository.findById(ruleCode).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, ruleCode) }
        val account = lockOrCreate(userId)
        if (!rule.enabled) return EarnResultDto(false, 0L, account.balance, EarnSkipReason.RULE_DISABLED)
        if (refId != null && transactionRepository.existsByUserIdAndRefId(userId, refId)) {
            return EarnResultDto(false, 0L, account.balance, EarnSkipReason.DUPLICATE)
        }
        rule.totalLimit?.let { limit ->
            if (transactionRepository.countByUserIdAndRuleCode(userId, rule.code) >= limit) {
                return EarnResultDto(false, 0L, account.balance, EarnSkipReason.TOTAL_LIMIT)
            }
        }
        rule.dailyLimit?.let { limit ->
            val startOfDay = LocalDate.now(clock).atStartOfDay()
            if (transactionRepository.countByUserIdAndRuleCodeAndCreatedDateGreaterThanEqual(userId, rule.code, startOfDay) >= limit) {
                return EarnResultDto(false, 0L, account.balance, EarnSkipReason.DAILY_LIMIT)
            }
        }
        val balance = account.apply(rule.points)
        transactionRepository.save(
            PointTransaction(userId, PointTransactionType.EARN, rule.points, balance, now(), ruleCode = rule.code, refId = refId, memo = memo ?: rule.name),
        )
        return EarnResultDto(true, rule.points, balance)
    }

    /** 출석 체크. 하루 한 번은 규칙(DAILY_CHECKIN 의 dailyLimit)이 막고, refId 로도 한 번 더 막는다. */
    fun checkIn(userId: String): EarnResultDto =
        earn(userId, CHECKIN_RULE, refId = "checkin:${LocalDate.now(clock)}")

    fun spend(userId: String, amount: Long, refId: String, memo: String? = null): SpendResultDto {
        if (amount <= 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, amount.toString())
        val account = lockOrCreate(userId)
        if (transactionRepository.existsByUserIdAndRefId(userId, refId)) {
            return SpendResultDto(false, 0L, account.balance)
        }
        if (account.balance < amount) throw CustomException(ErrorCode.INSUFFICIENT_POINT, "잔액 ${account.balance}, 요청 $amount")
        val balance = account.apply(-amount)
        transactionRepository.save(PointTransaction(userId, PointTransactionType.SPEND, -amount, balance, now(), refId = refId, memo = memo))
        return SpendResultDto(true, amount, balance)
    }

    /** 관리자 수동 조정. 회수는 잔액을 넘을 수 없다. */
    fun adjust(userId: String, request: AdjustRequestDto): PointBalanceDto {
        if (request.amount == 0L) throw CustomException(ErrorCode.INVALID_AMOUNT, "0")
        val account = lockOrCreate(userId)
        if (account.balance + request.amount < 0L) {
            throw CustomException(ErrorCode.INSUFFICIENT_POINT, "잔액 ${account.balance}, 조정 ${request.amount}")
        }
        val balance = account.apply(request.amount)
        transactionRepository.save(PointTransaction(userId, PointTransactionType.ADJUST, request.amount, balance, now(), memo = request.memo))
        return PointBalanceDto(userId, balance)
    }

    /**
     * 백오피스 목록. 검색어는 member-service 의 회원 검색(이름·이메일·userId)에 넘겨 걸린 회원들의 계정만 보여 준다.
     * 이름·이메일은 조회해 붙이고, member-service 가 응답하지 않으면 그 칸만 비운다.
     */
    @Transactional(readOnly = true)
    fun accounts(keyword: String?, pageable: Pageable): Page<AdminPointAccountDto> {
        if (keyword.isNullOrBlank()) {
            val page = accountRepository.findAll(pageable)
            val members = memberLookup.byUserIds(page.content.map { it.userId })
            return page.map { AdminPointAccountDto.of(it, members[it.userId]) }
        }
        val matched = memberLookup.search(keyword.trim())
        if (matched.isEmpty()) return Page.empty(pageable)
        val byUserId = matched.associateBy { it.userId!! }
        return accountRepository.findByUserIdIn(byUserId.keys, pageable).map { AdminPointAccountDto.of(it, byUserId[it.userId]) }
    }

    @Transactional(readOnly = true)
    fun account(userId: String): AdminPointAccountDto {
        val account = accountRepository.findByUserId(userId).orElseThrow { CustomException(ErrorCode.ACCOUNT_NOT_FOUND, userId) }
        return AdminPointAccountDto.of(account, memberLookup.byUserIds(listOf(userId))[userId])
    }

    /** 계정이 아직 없는 사용자의 이름·이메일. 백오피스 상세가 0 P 화면에서도 누구인지 보여 주는 데 쓴다. */
    @Transactional(readOnly = true)
    fun member(userId: String): MemberSummaryDto? = memberLookup.byUserIds(listOf(userId))[userId]

    @Transactional(readOnly = true)
    fun rules(): List<PointRuleDto> = ruleRepository.findAll().sortedBy { it.code }.map { PointRuleDto.of(it) }

    fun createRule(request: PointRuleCreateDto): PointRuleDto {
        if (ruleRepository.existsById(request.code)) throw CustomException(ErrorCode.RULE_ALREADY_EXISTS, request.code)
        val rule = ruleRepository.save(
            PointRule(request.code, request.name, request.points, request.dailyLimit, request.totalLimit, request.enabled),
        )
        return PointRuleDto.of(rule)
    }

    /** 원장은 규칙 코드를 문자열로 들고 있어 규칙을 지워도 과거 이력은 남는다. 출석 규칙만은 코드가 고정이라 지우지 못한다. */
    fun deleteRule(code: String) {
        if (code == CHECKIN_RULE) throw CustomException(ErrorCode.RULE_IN_USE, code)
        val rule = ruleRepository.findById(code).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, code) }
        ruleRepository.delete(rule)
    }

    fun updateRule(code: String, request: PointRuleUpdateDto): PointRuleDto {
        val rule = ruleRepository.findById(code).orElseThrow { CustomException(ErrorCode.RULE_NOT_FOUND, code) }
        rule.update(request.name, request.points, request.dailyLimit, request.totalLimit, request.enabled)
        return PointRuleDto.of(rule)
    }

    /** 잠근 계정을 돌려준다. 없으면 만들고 잠근다(첫 적립). */
    private fun lockOrCreate(userId: String): PointAccount =
        accountRepository.findByUserIdForUpdate(userId).orElseGet {
            accountRepository.saveAndFlush(PointAccount(userId))
            accountRepository.findByUserIdForUpdate(userId).orElseThrow()
        }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    companion object {
        const val CHECKIN_RULE = "DAILY_CHECKIN"
    }
}
