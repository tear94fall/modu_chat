package com.example.pointservice.application.usecase

import com.example.pointservice.application.common.exception.CustomException
import com.example.pointservice.application.common.exception.ErrorCode
import com.example.pointservice.application.domain.entity.PointTransactionType
import com.example.pointservice.application.domain.repository.rw.PointAccountRwRepository
import com.example.pointservice.application.domain.repository.rw.PointTransactionRwRepository
import com.example.pointservice.application.member.MemberLookup
import com.example.pointservice.application.member.MemberSummary
import com.example.pointservice.application.support.TestClock
import com.example.pointservice.application.support.TestClockConfig
import com.example.pointservice.application.usecase.command.AdjustCommand
import com.example.pointservice.application.usecase.command.CancelSpendCommand
import com.example.pointservice.application.usecase.command.CreatePointRuleCommand
import com.example.pointservice.application.usecase.command.EarnAmountCommand
import com.example.pointservice.application.usecase.command.EarnCommand
import com.example.pointservice.application.usecase.command.SpendCommand
import com.example.pointservice.application.usecase.command.TransactionRef
import com.example.pointservice.application.usecase.command.UpdatePointRuleCommand
import com.example.pointservice.application.usecase.result.EarnSkipReason
import com.example.pointservice.application.usecase.result.SpendCancelSkipReason
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/** H2 위에서 실제 저장소·트랜잭션으로 적립·사용·조정 규칙을 확인한다. 회원 조회 포트만 목이다. */
@SpringBootTest
@Import(TestClockConfig::class)
class PointUseCaseTest {

    @Autowired lateinit var pointUseCase: PointUseCase
    @Autowired lateinit var pointAccountUseCase: PointAccountUseCase
    @Autowired lateinit var pointRuleUseCase: PointRuleUseCase
    @Autowired lateinit var accountRepository: PointAccountRwRepository
    @Autowired lateinit var transactionRepository: PointTransactionRwRepository
    @Autowired lateinit var clock: TestClock
    @MockitoBean lateinit var memberLookup: MemberLookup

    private val user = "google-sub-1"

    @AfterEach
    fun cleanUp() {
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        pointRuleUseCase.update("DAILY_CHECKIN", UpdatePointRuleCommand("출석 체크", 10, dailyLimit = 1, enabled = true))
    }

    @Test
    @DisplayName("규칙 코드로 적립하면 잔액이 늘고 원장에 한 줄 남는다")
    fun earnAppliesRuleAndWritesLedger() {
        val result = pointUseCase.earn(EarnCommand(user, "SIGNUP"))

        assertThat(result.applied).isTrue()
        assertThat(result.amount).isEqualTo(100L)
        assertThat(result.balance).isEqualTo(100L)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(100L)
        val history = pointUseCase.history(user, PageRequest.of(0, 10)).content
        assertThat(history).hasSize(1)
        assertThat(history[0].type).isEqualTo(PointTransactionType.EARN)
        assertThat(history[0].ruleCode).isEqualTo("SIGNUP")
        assertThat(history[0].balanceAfter).isEqualTo(100L)
    }

    @Test
    @DisplayName("모르는 사용자 잔액은 0, 모르는 규칙은 404")
    fun unknownUserAndRule() {
        assertThat(pointUseCase.balance("nobody").balance).isEqualTo(0L)
        assertThatThrownBy { pointUseCase.earn(EarnCommand(user, "NOPE")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)
    }

    @Test
    @DisplayName("같은 refId 로 두 번 적립하면 두 번째는 적용되지 않는다")
    fun duplicateRefIdIsSkipped() {
        pointUseCase.earn(EarnCommand(user, "INVITE_FRIEND", refId = "invite:42"))
        val again = pointUseCase.earn(EarnCommand(user, "INVITE_FRIEND", refId = "invite:42"))

        assertThat(again.applied).isFalse()
        assertThat(again.reason).isEqualTo(EarnSkipReason.DUPLICATE)
        assertThat(again.balance).isEqualTo(50L)
    }

    @Test
    @DisplayName("금액 지정 적립은 상한 없이 적립되고 출처가 원장에 남으며 refId 로 멱등이다")
    fun earnAmount() {
        val first = pointUseCase.earnAmount(EarnAmountCommand(user, 1500L, "PURCHASE", "purchase:order:1", "구매 적립"))
        val second = pointUseCase.earnAmount(EarnAmountCommand(user, 700L, "PURCHASE", "purchase:order:2"))
        val again = pointUseCase.earnAmount(EarnAmountCommand(user, 1500L, "PURCHASE", "purchase:order:1"))

        assertThat(first.applied).isTrue()
        assertThat(first.balance).isEqualTo(1500L)
        assertThat(second.balance).isEqualTo(2200L)
        assertThat(again.applied).isFalse()
        assertThat(again.amount).isEqualTo(0L)
        assertThat(again.reason).isEqualTo(EarnSkipReason.DUPLICATE)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(2200L)
        val history = pointUseCase.history(user, PageRequest.of(0, 10)).content
        assertThat(history).hasSize(2)
        assertThat(history.map { it.type }).containsOnly(PointTransactionType.EARN)
        assertThat(history.map { it.reason }).containsOnly("PURCHASE")
        assertThat(history.map { it.ruleCode }).containsOnlyNulls()
        assertThat(history[1].memo).isEqualTo("구매 적립")
        assertThatThrownBy { pointUseCase.earnAmount(EarnAmountCommand(user, 0L, "PURCHASE", "purchase:order:3")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT)
    }

    @Test
    @DisplayName("전체 상한(가입 축하 1회)을 넘으면 적용되지 않는다")
    fun totalLimit() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        val second = pointUseCase.earn(EarnCommand(user, "SIGNUP"))

        assertThat(second.applied).isFalse()
        assertThat(second.reason).isEqualTo(EarnSkipReason.TOTAL_LIMIT)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(100L)
    }

    @Test
    @DisplayName("출석은 하루 한 번이고 다음 날 다시 된다")
    fun dailyCheckIn() {
        assertThat(pointUseCase.checkIn(user).applied).isTrue()
        val sameDay = pointUseCase.checkIn(user)
        assertThat(sameDay.applied).isFalse()
        assertThat(sameDay.reason).isEqualTo(EarnSkipReason.DUPLICATE)

        clock.plusDays(1)
        assertThat(pointUseCase.checkIn(user).applied).isTrue()
        assertThat(pointUseCase.balance(user).balance).isEqualTo(20L)
    }

    @Test
    @DisplayName("하루 상한은 refId 없이도 걸린다")
    fun dailyLimitWithoutRefId() {
        // 친구 초대: 하루 5회
        repeat(5) { assertThat(pointUseCase.earn(EarnCommand(user, "INVITE_FRIEND")).applied).isTrue() }
        val sixth = pointUseCase.earn(EarnCommand(user, "INVITE_FRIEND"))
        assertThat(sixth.applied).isFalse()
        assertThat(sixth.reason).isEqualTo(EarnSkipReason.DAILY_LIMIT)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(250L)
    }

    @Test
    @DisplayName("비활성 규칙은 적립하지 않는다")
    fun disabledRule() {
        pointRuleUseCase.update("DAILY_CHECKIN", UpdatePointRuleCommand("출석 체크", 10, dailyLimit = 1, enabled = false))
        val result = pointUseCase.checkIn(user)
        assertThat(result.applied).isFalse()
        assertThat(result.reason).isEqualTo(EarnSkipReason.RULE_DISABLED)
    }

    @Test
    @DisplayName("사용은 잔액 안에서만 되고 같은 refId 는 한 번만 차감된다")
    fun spend() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))

        val first = pointUseCase.spend(SpendCommand(user, 30L, refId = "order:1"))
        assertThat(first.applied).isTrue()
        assertThat(first.balance).isEqualTo(70L)

        val retry = pointUseCase.spend(SpendCommand(user, 30L, refId = "order:1"))
        assertThat(retry.applied).isFalse()
        assertThat(retry.balance).isEqualTo(70L)

        assertThatThrownBy { pointUseCase.spend(SpendCommand(user, 100L, refId = "order:2")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_POINT)
        assertThatThrownBy { pointUseCase.spend(SpendCommand(user, 0L, refId = "order:3")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(70L)
    }

    @Test
    @DisplayName("사용 취소는 사용 금액을 돌려주고 refund: 키로 REFUND 줄을 남긴다")
    fun cancelSpendRestoresBalance() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        pointUseCase.spend(SpendCommand(user, 30L, refId = "order:1"))

        val result = pointUseCase.cancelSpend(CancelSpendCommand(user, "order:1", "결제 실패"))

        assertThat(result.cancelled).isTrue()
        assertThat(result.reason).isNull()
        assertThat(result.amount).isEqualTo(30L)
        assertThat(result.balance).isEqualTo(100L)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(100L)
        val latest = pointUseCase.history(user, PageRequest.of(0, 10)).content[0]
        assertThat(latest.type).isEqualTo(PointTransactionType.REFUND)
        assertThat(latest.refId).isEqualTo("refund:order:1")
        assertThat(latest.amount).isEqualTo(30L)
        assertThat(latest.balanceAfter).isEqualTo(100L)
        assertThat(latest.memo).isEqualTo("결제 실패")
    }

    @Test
    @DisplayName("사용한 적 없는 refId 는 NO_SPEND — 원장·잔액·계정 모두 그대로")
    fun cancelSpendWithoutSpend() {
        val noAccount = pointUseCase.cancelSpend(CancelSpendCommand("nobody", "order:1"))
        assertThat(noAccount.cancelled).isFalse()
        assertThat(noAccount.reason).isEqualTo(SpendCancelSkipReason.NO_SPEND)
        assertThat(noAccount.balance).isEqualTo(0L)
        assertThat(accountRepository.findByUserId("nobody")).isEmpty

        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        pointUseCase.earnAmount(EarnAmountCommand(user, 20L, "PURCHASE", "purchase:order:9"))
        val noSpend = pointUseCase.cancelSpend(CancelSpendCommand(user, "order:9"))
        // 같은 refId 라도 SPEND 가 아니면(적립 줄) 되돌리지 않는다
        val notSpend = pointUseCase.cancelSpend(CancelSpendCommand(user, "purchase:order:9"))

        assertThat(noSpend.reason).isEqualTo(SpendCancelSkipReason.NO_SPEND)
        assertThat(noSpend.amount).isEqualTo(0L)
        assertThat(noSpend.balance).isEqualTo(120L)
        assertThat(notSpend.reason).isEqualTo(SpendCancelSkipReason.NO_SPEND)
        assertThat(transactionRepository.count()).isEqualTo(2L)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(120L)
    }

    @Test
    @DisplayName("주문 취소 환불(refund:order:…)이 먼저 됐으면 ALREADY_REFUNDED, 두 번 불러도 한 번만 돌려준다")
    fun cancelSpendAfterRefundOrTwice() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        pointUseCase.spend(SpendCommand(user, 40L, refId = "order:1"))
        pointUseCase.refund(SpendCommand(user, 40L, refId = "refund:order:1"))

        val afterRefund = pointUseCase.cancelSpend(CancelSpendCommand(user, "order:1"))
        assertThat(afterRefund.cancelled).isFalse()
        assertThat(afterRefund.reason).isEqualTo(SpendCancelSkipReason.ALREADY_REFUNDED)
        assertThat(afterRefund.balance).isEqualTo(100L)

        pointUseCase.spend(SpendCommand(user, 25L, refId = "order:2"))
        assertThat(pointUseCase.cancelSpend(CancelSpendCommand(user, "order:2")).cancelled).isTrue()
        val second = pointUseCase.cancelSpend(CancelSpendCommand(user, "order:2"))
        assertThat(second.reason).isEqualTo(SpendCancelSkipReason.ALREADY_REFUNDED)
        assertThat(second.amount).isEqualTo(0L)
        // 반대로 사용 취소가 먼저면 주문 취소 환불은 같은 키라 적용되지 않는다
        assertThat(pointUseCase.refund(SpendCommand(user, 25L, refId = "refund:order:2")).applied).isFalse()
        assertThat(pointUseCase.balance(user).balance).isEqualTo(100L)
    }

    @Test
    @DisplayName("사용 취소가 동시에 여러 번 와도 한 번만 돌려준다")
    fun cancelSpendConcurrently() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        pointUseCase.spend(SpendCommand(user, 60L, refId = "order:1"))
        val threads = 4
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val futures = (1..threads).map {
            pool.submit<Boolean> {
                ready.countDown()
                go.await()
                pointUseCase.cancelSpend(CancelSpendCommand(user, "order:1")).cancelled
            }
        }
        ready.await()
        go.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertThat(results.count { it }).isEqualTo(1)
        assertThat(pointUseCase.balance(user).balance).isEqualTo(100L)
        assertThat(pointUseCase.history(user, PageRequest.of(0, 10)).content.count { it.type == PointTransactionType.REFUND }).isEqualTo(1)
    }

    @Test
    @DisplayName("refId 로 찾기는 있는 쌍만 돌려주고, 다른 사용자의 같은 refId 는 섞이지 않으며, 500 개를 넘으면 거절한다")
    fun transactionsByRefs() {
        pointUseCase.earn(EarnCommand(user, "SIGNUP"))
        pointUseCase.earn(EarnCommand("other", "SIGNUP"))
        pointUseCase.spend(SpendCommand(user, 30L, refId = "order:1"))
        pointUseCase.spend(SpendCommand("other", 10L, refId = "order:2"))
        pointUseCase.cancelSpend(CancelSpendCommand(user, "order:1"))

        val found = pointUseCase.transactionsByRefs(
            listOf(
                TransactionRef(user, "order:1"),
                TransactionRef(user, "refund:order:1"),
                TransactionRef(user, "order:2"), // order:2 는 other 의 것
                TransactionRef("other", "order:2"),
                TransactionRef("other", "order:2"), // 중복 요청은 한 번만
                TransactionRef(user, "order:404"),
            ),
        )

        assertThat(found.map { Triple(it.userId, it.refId, it.amount) }).containsExactlyInAnyOrder(
            Triple(user, "order:1", -30L),
            Triple(user, "refund:order:1", 30L),
            Triple("other", "order:2", -10L),
        )
        assertThat(found.first { it.refId == "refund:order:1" }.type).isEqualTo(PointTransactionType.REFUND)
        assertThat(found.map { it.createdDate }).doesNotContainNull()
        assertThat(pointUseCase.transactionsByRefs(emptyList())).isEmpty()
        // 100 개씩 나눠 묻는다 — 경계를 넘는 요청도 빠짐없이
        val many = (1..250).map { TransactionRef(user, "missing:$it") } + TransactionRef(user, "order:1")
        assertThat(pointUseCase.transactionsByRefs(many).map { it.refId }).containsExactly("order:1")
        assertThatThrownBy { pointUseCase.transactionsByRefs((1..501).map { TransactionRef(user, "r:$it") }) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("관리자 조정은 지급·회수 모두 되고 잔액 아래로는 회수하지 못한다")
    fun adjust() {
        assertThat(pointUseCase.adjust(AdjustCommand(user, 500L, "이벤트 보상")).balance).isEqualTo(500L)
        assertThat(pointUseCase.adjust(AdjustCommand(user, -200L, "오지급 회수")).balance).isEqualTo(300L)
        assertThatThrownBy { pointUseCase.adjust(AdjustCommand(user, -301L, "너무 많이")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_POINT)

        val history = pointUseCase.history(user, PageRequest.of(0, 10)).content
        assertThat(history.map { it.type }).containsExactly(PointTransactionType.ADJUST, PointTransactionType.ADJUST)
        assertThat(history.map { it.amount }).containsExactly(-200L, 500L)
    }

    @Test
    @DisplayName("기본 규칙이 들어 있고 백오피스가 점수를 바꿀 수 있다")
    fun rules() {
        assertThat(pointRuleUseCase.rules().map { it.code }).contains("SIGNUP", "DAILY_CHECKIN", "INVITE_FRIEND", "PROFILE_COMPLETE", "FIRST_CHAT")
        val updated = pointRuleUseCase.update("DAILY_CHECKIN", UpdatePointRuleCommand("출석", 15, dailyLimit = 1, enabled = true))
        assertThat(updated.points).isEqualTo(15L)
        assertThat(pointUseCase.checkIn(user).amount).isEqualTo(15L)
    }

    @Test
    @DisplayName("규칙을 추가·삭제할 수 있고, 지운 규칙의 이력은 남으며, 출석 규칙은 못 지운다")
    fun createAndDeleteRule() {
        val created = pointRuleUseCase.create(CreatePointRuleCommand("REVIEW_WRITE", "리뷰 작성", 5, dailyLimit = 3))
        assertThat(created.code).isEqualTo("REVIEW_WRITE")
        assertThat(pointRuleUseCase.rules().map { it.code }).contains("REVIEW_WRITE")
        assertThatThrownBy { pointRuleUseCase.create(CreatePointRuleCommand("REVIEW_WRITE", "중복", 1)) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_ALREADY_EXISTS)

        assertThat(pointUseCase.earn(EarnCommand(user, "REVIEW_WRITE")).applied).isTrue()
        pointRuleUseCase.delete("REVIEW_WRITE")
        assertThat(pointRuleUseCase.rules().map { it.code }).doesNotContain("REVIEW_WRITE")
        assertThat(pointUseCase.history(user, PageRequest.of(0, 10)).content.map { it.ruleCode }).contains("REVIEW_WRITE")
        assertThatThrownBy { pointUseCase.earn(EarnCommand(user, "REVIEW_WRITE")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)

        assertThatThrownBy { pointRuleUseCase.delete("DAILY_CHECKIN") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_IN_USE)
        assertThatThrownBy { pointRuleUseCase.delete("NOPE") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)
    }

    @Test
    @DisplayName("관리자 목록은 회원 이름·이메일을 붙이고, 검색은 member-service 회원 검색 결과로 거른다")
    fun adminAccounts() {
        pointUseCase.earn(EarnCommand("alpha-1", "SIGNUP"))
        pointUseCase.earn(EarnCommand("beta-2", "SIGNUP"))
        val alice = MemberSummary("alpha-1", "앨리스", "alice@example.com")
        val bob = MemberSummary("beta-2", "밥", "bob@example.com")
        whenever(memberLookup.byUserIds(any())).thenAnswer { inv ->
            val ids = inv.getArgument<Collection<String>>(0)
            listOf(alice, bob).filter { it.userId in ids }.associateBy { it.userId }
        }
        whenever(memberLookup.search(eq("앨리"))).thenReturn(listOf(alice))
        whenever(memberLookup.search(eq("없음"))).thenReturn(emptyList())

        val all = pointAccountUseCase.accounts(null, PageRequest.of(0, 10))
        assertThat(all.totalElements).isEqualTo(2)
        assertThat(all.content.map { it.username }).containsExactlyInAnyOrder("앨리스", "밥")
        assertThat(all.content.first { it.userId == "beta-2" }.email).isEqualTo("bob@example.com")

        val searched = pointAccountUseCase.accounts("앨리", PageRequest.of(0, 10))
        assertThat(searched.content.map { it.userId }).containsExactly("alpha-1")
        assertThat(pointAccountUseCase.accounts("없음", PageRequest.of(0, 10)).totalElements).isEqualTo(0)

        assertThat(pointAccountUseCase.account("beta-2").balance).isEqualTo(100L)
        assertThat(pointAccountUseCase.account("beta-2").username).isEqualTo("밥")
        assertThatThrownBy { pointAccountUseCase.account("nobody") }.isInstanceOf(CustomException::class.java)
    }

    @Test
    @DisplayName("회원 조회가 빈 결과여도(member-service 장애) 목록은 이름 없이 나온다")
    fun adminAccountsWithoutMemberService() {
        pointUseCase.earn(EarnCommand("alpha-1", "SIGNUP"))
        whenever(memberLookup.byUserIds(any())).thenReturn(emptyMap())
        val all = pointAccountUseCase.accounts(null, PageRequest.of(0, 10))
        assertThat(all.totalElements).isEqualTo(1)
        assertThat(all.content[0].username).isNull()
        assertThat(all.content[0].balance).isEqualTo(100L)
    }
}
