package com.example.pointservice.point.service

import com.example.pointservice.TestClock
import com.example.pointservice.TestClockConfig
import com.example.pointservice.api.dto.AdjustRequestDto
import com.example.pointservice.api.dto.EarnSkipReason
import com.example.pointservice.api.dto.PointRuleCreateDto
import com.example.pointservice.api.dto.PointRuleUpdateDto
import com.example.pointservice.common.exception.CustomException
import com.example.pointservice.common.exception.ErrorCode
import com.example.pointservice.member.MemberFeignClient
import com.example.pointservice.member.MemberPageDto
import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.entity.PointTransactionType
import com.example.pointservice.point.repository.PointAccountRepository
import com.example.pointservice.point.repository.PointTransactionRepository
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.data.domain.PageRequest

/** H2 위에서 실제 저장소·트랜잭션으로 적립·사용·조정 규칙을 확인한다. */
@SpringBootTest
@Import(TestClockConfig::class)
class PointServiceTest {

    @Autowired lateinit var pointService: PointService
    @Autowired lateinit var accountRepository: PointAccountRepository
    @Autowired lateinit var transactionRepository: PointTransactionRepository
    @Autowired lateinit var clock: TestClock
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient

    private val user = "google-sub-1"

    @AfterEach
    fun cleanUp() {
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        pointService.updateRule("DAILY_CHECKIN", PointRuleUpdateDto("출석 체크", 10, dailyLimit = 1, enabled = true))
    }

    @Test
    @DisplayName("규칙 코드로 적립하면 잔액이 늘고 원장에 한 줄 남는다")
    fun earnAppliesRuleAndWritesLedger() {
        val result = pointService.earn(user, "SIGNUP")

        assertThat(result.applied).isTrue()
        assertThat(result.amount).isEqualTo(100L)
        assertThat(result.balance).isEqualTo(100L)
        assertThat(pointService.balance(user).balance).isEqualTo(100L)
        val history = pointService.history(user, PageRequest.of(0, 10)).content
        assertThat(history).hasSize(1)
        assertThat(history[0].type).isEqualTo(PointTransactionType.EARN)
        assertThat(history[0].ruleCode).isEqualTo("SIGNUP")
        assertThat(history[0].balanceAfter).isEqualTo(100L)
    }

    @Test
    @DisplayName("모르는 사용자 잔액은 0, 모르는 규칙은 404")
    fun unknownUserAndRule() {
        assertThat(pointService.balance("nobody").balance).isEqualTo(0L)
        assertThatThrownBy { pointService.earn(user, "NOPE") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)
    }

    @Test
    @DisplayName("같은 refId 로 두 번 적립하면 두 번째는 적용되지 않는다")
    fun duplicateRefIdIsSkipped() {
        pointService.earn(user, "INVITE_FRIEND", refId = "invite:42")
        val again = pointService.earn(user, "INVITE_FRIEND", refId = "invite:42")

        assertThat(again.applied).isFalse()
        assertThat(again.reason).isEqualTo(EarnSkipReason.DUPLICATE)
        assertThat(again.balance).isEqualTo(50L)
    }

    @Test
    @DisplayName("전체 상한(가입 축하 1회)을 넘으면 적용되지 않는다")
    fun totalLimit() {
        pointService.earn(user, "SIGNUP")
        val second = pointService.earn(user, "SIGNUP")

        assertThat(second.applied).isFalse()
        assertThat(second.reason).isEqualTo(EarnSkipReason.TOTAL_LIMIT)
        assertThat(pointService.balance(user).balance).isEqualTo(100L)
    }

    @Test
    @DisplayName("출석은 하루 한 번이고 다음 날 다시 된다")
    fun dailyCheckIn() {
        assertThat(pointService.checkIn(user).applied).isTrue()
        val sameDay = pointService.checkIn(user)
        assertThat(sameDay.applied).isFalse()
        assertThat(sameDay.reason).isEqualTo(EarnSkipReason.DUPLICATE)

        clock.plusDays(1)
        assertThat(pointService.checkIn(user).applied).isTrue()
        assertThat(pointService.balance(user).balance).isEqualTo(20L)
    }

    @Test
    @DisplayName("하루 상한은 refId 없이도 걸린다")
    fun dailyLimitWithoutRefId() {
        // 친구 초대: 하루 5회
        repeat(5) { assertThat(pointService.earn(user, "INVITE_FRIEND").applied).isTrue() }
        val sixth = pointService.earn(user, "INVITE_FRIEND")
        assertThat(sixth.applied).isFalse()
        assertThat(sixth.reason).isEqualTo(EarnSkipReason.DAILY_LIMIT)
        assertThat(pointService.balance(user).balance).isEqualTo(250L)
    }

    @Test
    @DisplayName("비활성 규칙은 적립하지 않는다")
    fun disabledRule() {
        pointService.updateRule("DAILY_CHECKIN", PointRuleUpdateDto("출석 체크", 10, dailyLimit = 1, enabled = false))
        val result = pointService.checkIn(user)
        assertThat(result.applied).isFalse()
        assertThat(result.reason).isEqualTo(EarnSkipReason.RULE_DISABLED)
    }

    @Test
    @DisplayName("사용은 잔액 안에서만 되고 같은 refId 는 한 번만 차감된다")
    fun spend() {
        pointService.earn(user, "SIGNUP")

        val first = pointService.spend(user, 30L, refId = "order:1")
        assertThat(first.applied).isTrue()
        assertThat(first.balance).isEqualTo(70L)

        val retry = pointService.spend(user, 30L, refId = "order:1")
        assertThat(retry.applied).isFalse()
        assertThat(retry.balance).isEqualTo(70L)

        assertThatThrownBy { pointService.spend(user, 100L, refId = "order:2") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_POINT)
        assertThatThrownBy { pointService.spend(user, 0L, refId = "order:3") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT)
        assertThat(pointService.balance(user).balance).isEqualTo(70L)
    }

    @Test
    @DisplayName("관리자 조정은 지급·회수 모두 되고 잔액 아래로는 회수하지 못한다")
    fun adjust() {
        assertThat(pointService.adjust(user, AdjustRequestDto(500L, "이벤트 보상")).balance).isEqualTo(500L)
        assertThat(pointService.adjust(user, AdjustRequestDto(-200L, "오지급 회수")).balance).isEqualTo(300L)
        assertThatThrownBy { pointService.adjust(user, AdjustRequestDto(-301L, "너무 많이")) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_POINT)

        val history = pointService.history(user, PageRequest.of(0, 10)).content
        assertThat(history.map { it.type }).containsExactly(PointTransactionType.ADJUST, PointTransactionType.ADJUST)
        assertThat(history.map { it.amount }).containsExactly(-200L, 500L)
    }

    @Test
    @DisplayName("기본 규칙이 들어 있고 백오피스가 점수를 바꿀 수 있다")
    fun rules() {
        assertThat(pointService.rules().map { it.code }).contains("SIGNUP", "DAILY_CHECKIN", "INVITE_FRIEND", "PROFILE_COMPLETE", "FIRST_CHAT")
        val updated = pointService.updateRule("DAILY_CHECKIN", PointRuleUpdateDto("출석", 15, dailyLimit = 1, enabled = true))
        assertThat(updated.points).isEqualTo(15L)
        assertThat(pointService.checkIn(user).amount).isEqualTo(15L)
    }

    @Test
    @DisplayName("규칙을 추가·삭제할 수 있고, 지운 규칙의 이력은 남으며, 출석 규칙은 못 지운다")
    fun createAndDeleteRule() {
        val created = pointService.createRule(PointRuleCreateDto("REVIEW_WRITE", "리뷰 작성", 5, dailyLimit = 3))
        assertThat(created.code).isEqualTo("REVIEW_WRITE")
        assertThat(pointService.rules().map { it.code }).contains("REVIEW_WRITE")
        assertThatThrownBy { pointService.createRule(PointRuleCreateDto("REVIEW_WRITE", "중복", 1)) }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_ALREADY_EXISTS)

        assertThat(pointService.earn(user, "REVIEW_WRITE").applied).isTrue()
        pointService.deleteRule("REVIEW_WRITE")
        assertThat(pointService.rules().map { it.code }).doesNotContain("REVIEW_WRITE")
        assertThat(pointService.history(user, PageRequest.of(0, 10)).content.map { it.ruleCode }).contains("REVIEW_WRITE")
        assertThatThrownBy { pointService.earn(user, "REVIEW_WRITE") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)

        assertThatThrownBy { pointService.deleteRule("DAILY_CHECKIN") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_IN_USE)
        assertThatThrownBy { pointService.deleteRule("NOPE") }
            .isInstanceOf(CustomException::class.java)
            .extracting("errorCode").isEqualTo(ErrorCode.RULE_NOT_FOUND)
    }

    @Test
    @DisplayName("관리자 목록은 회원 이름·이메일을 붙이고, 검색은 member-service 회원 검색 결과로 거른다")
    fun adminAccounts() {
        pointService.earn("alpha-1", "SIGNUP")
        pointService.earn("beta-2", "SIGNUP")
        whenever(memberFeignClient.getMembersByUserId(any())).thenAnswer { inv ->
            (inv.getArgument(0) as List<*>).mapNotNull { id ->
                when (id) {
                    "alpha-1" -> MemberSummaryDto("alpha-1", "앨리스", "alice@example.com")
                    "beta-2" -> MemberSummaryDto("beta-2", "밥", "bob@example.com")
                    else -> null
                }
            }
        }
        whenever(memberFeignClient.searchMembers(eq("앨리"), any(), any()))
            .thenReturn(MemberPageDto(listOf(MemberSummaryDto("alpha-1", "앨리스", "alice@example.com"))))
        whenever(memberFeignClient.searchMembers(eq("없음"), any(), any())).thenReturn(MemberPageDto(emptyList()))

        val all = pointService.accounts(null, PageRequest.of(0, 10))
        assertThat(all.totalElements).isEqualTo(2)
        assertThat(all.content.map { it.username }).containsExactlyInAnyOrder("앨리스", "밥")
        assertThat(all.content.first { it.userId == "beta-2" }.email).isEqualTo("bob@example.com")

        val searched = pointService.accounts("앨리", PageRequest.of(0, 10))
        assertThat(searched.content.map { it.userId }).containsExactly("alpha-1")
        assertThat(pointService.accounts("없음", PageRequest.of(0, 10)).totalElements).isEqualTo(0)

        assertThat(pointService.account("beta-2").balance).isEqualTo(100L)
        assertThat(pointService.account("beta-2").username).isEqualTo("밥")
        assertThatThrownBy { pointService.account("nobody") }.isInstanceOf(CustomException::class.java)
    }

    @Test
    @DisplayName("member-service 가 죽어도 목록은 이름 없이 나온다")
    fun adminAccountsWithoutMemberService() {
        pointService.earn("alpha-1", "SIGNUP")
        whenever(memberFeignClient.getMembersByUserId(any())).thenThrow(RuntimeException("member-service down"))
        val all = pointService.accounts(null, PageRequest.of(0, 10))
        assertThat(all.totalElements).isEqualTo(1)
        assertThat(all.content[0].username).isNull()
        assertThat(all.content[0].balance).isEqualTo(100L)
    }
}
