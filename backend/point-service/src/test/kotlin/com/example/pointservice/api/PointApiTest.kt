package com.example.pointservice.api

import com.example.pointservice.member.MemberFeignClient
import com.example.pointservice.member.MemberPageDto
import com.example.pointservice.member.MemberSummaryDto
import com.example.pointservice.point.repository.PointAccountRepository
import com.example.pointservice.point.repository.PointTransactionRepository
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
    @Autowired lateinit var accountRepository: PointAccountRepository
    @Autowired lateinit var transactionRepository: PointTransactionRepository
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient

    private val token = "test-internal-token"
    private val user = "google-sub-9"

    @AfterEach
    fun cleanUp() {
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
    }

    @Test
    fun public_withoutUserHeader_is401() {
        mockMvc.perform(get("/api-public/point/me")).andExpect(status().isUnauthorized)
        mockMvc.perform(post("/api-public/point/me/checkin")).andExpect(status().isUnauthorized)
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
        mockMvc.perform(get("/api-internal/point/$user/history").header("X-Internal-Token", token).param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content[0].type").value("SPEND"))
            .andExpect(jsonPath("$.content[0].amount").value(-40))
            .andExpect(jsonPath("$.content[0].balanceAfter").value(60))
        mockMvc.perform(get("/api-internal/point/$user/history")).andExpect(status().isForbidden)
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
