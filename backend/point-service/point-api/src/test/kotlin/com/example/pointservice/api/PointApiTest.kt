package com.example.pointservice.api

import com.example.pointservice.api.member.MemberFeignClient
import com.example.pointservice.api.member.MemberPageDto
import com.example.pointservice.api.member.MemberSummaryDto
import com.example.pointservice.application.domain.repository.rw.PointAccountRwRepository
import com.example.pointservice.application.domain.repository.rw.PointTransactionRwRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 세 계층(공개·내부·관리자)의 인증 규칙과 응답 형태. */
@SpringBootTest
@AutoConfigureMockMvc
class PointApiTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var accountRepository: PointAccountRwRepository
    @Autowired lateinit var transactionRepository: PointTransactionRwRepository
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient

    private val token = "test-internal-token"
    private val user = "google-sub-9"

    @AfterEach
    fun cleanUp() {
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
    }

    /** 게이트웨이를 거치지 않아 X-Auth-User-Id 가 없는 요청은 본인 확인을 할 수 없으므로 403. */
    @Test
    fun public_withoutUserHeader_is403() {
        mockMvc.perform(get("/api-public/point/me"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").isNotEmpty)
        mockMvc.perform(get("/api-public/point/me").header("X-Auth-User-Id", " ")).andExpect(status().isForbidden)
        mockMvc.perform(get("/api-public/point/me/history")).andExpect(status().isForbidden)
        mockMvc.perform(post("/api-public/point/me/checkin")).andExpect(status().isForbidden)
        mockMvc.perform(get("/api-internal/point/$user/balance").header("X-Internal-Token", token))
            .andExpect(jsonPath("$.balance").value(0))
    }

    /** 오류 본문은 message 와 code 뿐이다. 읽을 수 없는 본문은 400, 없는 계정은 404. */
    @Test
    fun errors_haveMessageAndCode() {
        mockMvc.perform(
            post("/api-internal/point/spend").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":0,"refId":"order:0"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_AMOUNT"))
            .andExpect(jsonPath("$.message").isNotEmpty)
            .andExpect(jsonPath("$.status").doesNotExist())
        mockMvc.perform(
            post("/api-internal/point/spend").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":"many"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(get("/api-admin/point/accounts/nobody").header("X-Internal-Token", token))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
        mockMvc.perform(get("/api-admin/point/accounts").param("page", "x").header("X-Internal-Token", token))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(delete("/api-internal/point/spend").header("X-Internal-Token", token))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
    }

    @Test
    fun public_checkInAndBalanceAndHistory() {
        mockMvc.perform(post("/api-public/point/me/checkin").header("X-Auth-User-Id", user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.amount").value(10))
            .andExpect(jsonPath("$.balance").value(10))
        mockMvc.perform(post("/api-public/point/me/checkin").header("X-Auth-User-Id", user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(false))
            .andExpect(jsonPath("$.reason").value("DUPLICATE"))
        mockMvc.perform(get("/api-public/point/me").header("X-Auth-User-Id", user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(user))
            .andExpect(jsonPath("$.balance").value(10))
        mockMvc.perform(get("/api-public/point/me/history").header("X-Auth-User-Id", user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].ruleCode").value("DAILY_CHECKIN"))
    }

    @Test
    fun internal_withoutToken_is403() {
        mockMvc.perform(post("/api-internal/point/earn").contentType(MediaType.APPLICATION_JSON).content("""{"userId":"$user","ruleCode":"SIGNUP"}"""))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/api-admin/point/rules")).andExpect(status().isForbidden)
    }

    @Test
    fun internal_earnSpendAndErrors() {
        mockMvc.perform(
            post("/api-internal/point/earn").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","ruleCode":"SIGNUP","refId":"signup:$user"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.balance").value(100))
        mockMvc.perform(
            post("/api-internal/point/earn").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","ruleCode":"NOPE"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"))
        mockMvc.perform(
            post("/api-internal/point/earn").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"","ruleCode":"SIGNUP"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        mockMvc.perform(
            post("/api-internal/point/spend").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":40,"refId":"order:1","memo":"주문 할인"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.balance").value(60))
        mockMvc.perform(
            post("/api-internal/point/spend").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":100,"refId":"order:2"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"))
        mockMvc.perform(get("/api-internal/point/$user/balance").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(60))
        // 환불: 되돌리고, 같은 refId 는 한 번만
        mockMvc.perform(
            post("/api-internal/point/refund").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":40,"refId":"refund:order:1","memo":"주문 취소"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.balance").value(100))
        mockMvc.perform(
            post("/api-internal/point/refund").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","amount":40,"refId":"refund:order:1"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(false))
            .andExpect(jsonPath("$.balance").value(100))
        mockMvc.perform(get("/api-internal/point/$user/history").header("X-Internal-Token", token).param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content[0].type").value("REFUND"))
            .andExpect(jsonPath("$.content[0].amount").value(40))
            .andExpect(jsonPath("$.content[0].balanceAfter").value(100))
        mockMvc.perform(get("/api-internal/point/$user/history")).andExpect(status().isForbidden)
    }

    @Test
    fun internal_spendCancelAndRefs() {
        fun internalPost(path: String, body: String) =
            mockMvc.perform(post(path).header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
        internalPost("/api-internal/point/earn", """{"userId":"$user","ruleCode":"SIGNUP"}""")
        internalPost("/api-internal/point/spend", """{"userId":"$user","amount":30,"refId":"order:20261009-ABC123"}""")
            .andExpect(jsonPath("$.balance").value(70))

        val cancel = """{"userId":"$user","refId":"order:20261009-ABC123","memo":"결제 실패"}"""
        mockMvc.perform(post("/api-internal/point/spend/cancel").contentType(MediaType.APPLICATION_JSON).content(cancel))
            .andExpect(status().isForbidden)
        internalPost("/api-internal/point/spend/cancel", cancel)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cancelled").value(true))
            .andExpect(jsonPath("$.reason").doesNotExist())
            .andExpect(jsonPath("$.amount").value(30))
            .andExpect(jsonPath("$.balance").value(100))
        internalPost("/api-internal/point/spend/cancel", cancel)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cancelled").value(false))
            .andExpect(jsonPath("$.reason").value("ALREADY_REFUNDED"))
            .andExpect(jsonPath("$.amount").value(0))
            .andExpect(jsonPath("$.balance").value(100))
        internalPost("/api-internal/point/spend/cancel", """{"userId":"$user","refId":"order:never"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cancelled").value(false))
            .andExpect(jsonPath("$.reason").value("NO_SPEND"))
            .andExpect(jsonPath("$.balance").value(100))
        internalPost("/api-internal/point/spend/cancel", """{"userId":"$user","refId":""}""")
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        internalPost("/api-internal/point/spend/cancel", """{"userId":"$user","refId":"${"x".repeat(122)}"}""")
            .andExpect(status().isBadRequest)

        internalPost(
            "/api-internal/point/refs",
            """{"refs":[{"userId":"$user","refId":"order:20261009-ABC123"},{"userId":"$user","refId":"refund:order:20261009-ABC123"},""" +
                """{"userId":"$user","refId":"order:never"}]}""",
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions.length()").value(2))
            .andExpect(jsonPath("$.transactions[?(@.refId=='order:20261009-ABC123')].type").value("SPEND"))
            .andExpect(jsonPath("$.transactions[?(@.refId=='order:20261009-ABC123')].amount").value(-30))
            .andExpect(jsonPath("$.transactions[?(@.refId=='refund:order:20261009-ABC123')].type").value("REFUND"))
            .andExpect(jsonPath("$.transactions[?(@.refId=='refund:order:20261009-ABC123')].amount").value(30))
            .andExpect(jsonPath("$.transactions[0].userId").value(user))
            .andExpect(jsonPath("$.transactions[0].createdDate").isNotEmpty)
        val tooMany = (1..501).joinToString(",", "{\"refs\":[", "]}") { """{"userId":"$user","refId":"r:$it"}""" }
        internalPost("/api-internal/point/refs", tooMany)
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        val max = (1..500).joinToString(",", "{\"refs\":[", "]}") { """{"userId":"$user","refId":"r:$it"}""" }
        internalPost("/api-internal/point/refs", max)
            .andExpect(status().isOk).andExpect(jsonPath("$.transactions.length()").value(0))
        internalPost("/api-internal/point/refs", """{"refs":[{"userId":"","refId":"r"}]}""").andExpect(status().isBadRequest)
        mockMvc.perform(post("/api-internal/point/refs").contentType(MediaType.APPLICATION_JSON).content("""{"refs":[]}"""))
            .andExpect(status().isForbidden)
    }

    @Test
    fun internal_earnAmount_idempotentAndValidated() {
        val body = """{"userId":"$user","amount":1234,"reason":"PURCHASE","refId":"purchase:order:77","memo":"구매 적립"}"""
        mockMvc.perform(post("/api-internal/point/earn-amount").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
        mockMvc.perform(post("/api-internal/point/earn-amount").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(true))
            .andExpect(jsonPath("$.amount").value(1234))
            .andExpect(jsonPath("$.balance").value(1234))
            .andExpect(jsonPath("$.reason").doesNotExist())
        mockMvc.perform(post("/api-internal/point/earn-amount").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.applied").value(false))
            .andExpect(jsonPath("$.amount").value(0))
            .andExpect(jsonPath("$.balance").value(1234))
            .andExpect(jsonPath("$.reason").value("DUPLICATE"))
        // 같은 refId 는 규칙 적립과도 공유된다
        mockMvc.perform(
            post("/api-internal/point/earn").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":"$user","ruleCode":"SIGNUP","refId":"purchase:order:77"}"""),
        )
            .andExpect(jsonPath("$.reason").value("DUPLICATE"))
        listOf(
            """{"userId":"$user","amount":0,"reason":"PURCHASE","refId":"r1"}""",
            """{"userId":"$user","amount":1000001,"reason":"PURCHASE","refId":"r2"}""",
            """{"userId":"$user","amount":10,"reason":"","refId":"r3"}""",
            """{"userId":"$user","amount":10,"reason":"${"X".repeat(31)}","refId":"r4"}""",
            """{"userId":"$user","amount":10,"reason":"PURCHASE","refId":""}""",
            """{"userId":"","amount":10,"reason":"PURCHASE","refId":"r5"}""",
        ).forEach { bad ->
            mockMvc.perform(post("/api-internal/point/earn-amount").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        }
        mockMvc.perform(get("/api-internal/point/$user/history").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].type").value("EARN"))
            .andExpect(jsonPath("$.content[0].amount").value(1234))
            .andExpect(jsonPath("$.content[0].reason").value("PURCHASE"))
            .andExpect(jsonPath("$.content[0].ruleCode").doesNotExist())
            .andExpect(jsonPath("$.content[0].refId").value("purchase:order:77"))
            .andExpect(jsonPath("$.content[0].memo").value("구매 적립"))
    }

    @Test
    fun admin_adjustRulesAndAccounts() {
        mockMvc.perform(
            post("/api-admin/point/accounts/$user/adjust").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":300,"memo":"이벤트 보상"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(300))
        mockMvc.perform(
            post("/api-admin/point/accounts/$user/adjust").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":-10,"memo":""}"""),
        )
            .andExpect(status().isBadRequest)
        whenever(memberFeignClient.searchMembers(eq("준섭"), any(), any()))
            .thenReturn(MemberPageDto(listOf(MemberSummaryDto(user, "임준섭", "joon@example.com"))))
        whenever(memberFeignClient.getMembersByUserId(any())).thenReturn(listOf(MemberSummaryDto(user, "임준섭", "joon@example.com")))
        mockMvc.perform(get("/api-admin/point/accounts").param("keyword", "준섭").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].userId").value(user))
            .andExpect(jsonPath("$.content[0].username").value("임준섭"))
            .andExpect(jsonPath("$.content[0].email").value("joon@example.com"))
        mockMvc.perform(get("/api-admin/point/accounts/$user/member").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("임준섭"))
        mockMvc.perform(get("/api-admin/point/accounts/$user/history").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].type").value("ADJUST"))
            .andExpect(jsonPath("$.content[0].memo").value("이벤트 보상"))
        mockMvc.perform(get("/api-admin/point/rules").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code=='SIGNUP')].points").value(100))
        mockMvc.perform(
            put("/api-admin/point/rules/FIRST_CHAT").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"첫 메시지","points":25,"totalLimit":1,"enabled":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.points").value(25))
        mockMvc.perform(
            put("/api-admin/point/rules/NOPE").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"x","points":1,"enabled":true}"""),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun admin_createAndDeleteRule() {
        mockMvc.perform(
            post("/api-admin/point/rules").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"REVIEW_WRITE","name":"리뷰 작성","points":5,"dailyLimit":3,"enabled":true}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("REVIEW_WRITE"))
            .andExpect(jsonPath("$.dailyLimit").value(3))
        mockMvc.perform(
            post("/api-admin/point/rules").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"REVIEW_WRITE","name":"중복","points":1}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RULE_ALREADY_EXISTS"))
        mockMvc.perform(
            post("/api-admin/point/rules").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"bad code","name":"x","points":1}"""),
        )
            .andExpect(status().isBadRequest)
        mockMvc.perform(delete("/api-admin/point/rules/REVIEW_WRITE").header("X-Internal-Token", token))
            .andExpect(status().isNoContent)
        mockMvc.perform(delete("/api-admin/point/rules/REVIEW_WRITE").header("X-Internal-Token", token))
            .andExpect(status().isNotFound)
        mockMvc.perform(delete("/api-admin/point/rules/DAILY_CHECKIN").header("X-Internal-Token", token))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RULE_IN_USE"))
    }
}
