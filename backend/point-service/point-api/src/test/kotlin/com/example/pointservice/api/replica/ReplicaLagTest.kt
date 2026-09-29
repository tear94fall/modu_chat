package com.example.pointservice.api.replica

import com.example.pointservice.api.member.MemberFeignClient
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 "쓰고 바로 읽는" 흐름이 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 빈 원장의 master 를 통째로(스키마+데이터) 복사해 두고, 그 뒤로는
 * 아무것도 복제하지 않는다. 그래서 테스트 안에서 쓴 값은 master 에만 있다 — 잔액·원장·멱등 검사가 레플리카를 읽으면
 * 0 원·빈 목록·중복 적립으로 드러난다. 거꾸로 둘러보기(목록)는 레플리카를 읽으므로 옛 모습이 보여야 한다.
 */
@SpringBootTest(
    properties = ["spring.datasource.replica.url=jdbc:h2:mem:point_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1"],
)
@AutoConfigureMockMvc
class ReplicaLagTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient

    private lateinit var master: JdbcTemplate
    private lateinit var replica: JdbcTemplate

    private val token = "test-internal-token"
    private val user = "lag-user-1"

    @Autowired
    fun dataSources(
        @Qualifier("rwHikariDataSource") masterDataSource: DataSource,
        @Qualifier("roHikariDataSource") replicaDataSource: DataSource,
    ) {
        master = JdbcTemplate(masterDataSource)
        replica = JdbcTemplate(replicaDataSource)
    }

    @BeforeEach
    fun freezeReplica() {
        clearLedger()
        val script = master.queryForList("SCRIPT", String::class.java)
        replica.execute("DROP ALL OBJECTS")
        script.forEach { replica.execute(it) }
    }

    @AfterEach
    fun clearLedger() {
        master.update("delete from point_transaction")
        master.update("delete from point_account")
        master.update("delete from point_rule where rule_code = 'LAG_RULE'")
        master.update("update point_rule set points = 10, enabled = true where rule_code = 'DAILY_CHECKIN'")
    }

    private fun replicaCount(table: String): Long =
        requireNotNull(replica.queryForObject("select count(*) from $table", Long::class.java))

    private fun internalPost(path: String, body: String): ResultActions =
        mockMvc.perform(post(path).header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))

    private fun internalGet(path: String): ResultActions = mockMvc.perform(get(path).header("X-Internal-Token", token))

    @Test
    fun checkout_earnSpendRefund_thenBalanceAndHistory_readFresh() {
        internalPost("/api-internal/point/earn-amount", """{"userId":"$user","amount":1000,"reason":"PURCHASE","refId":"purchase:order:1"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.balance").value(1000))
        internalPost("/api-internal/point/spend", """{"userId":"$user","amount":300,"refId":"order:2"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.balance").value(700))
        internalPost("/api-internal/point/refund", """{"userId":"$user","amount":300,"refId":"refund:order:2"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.balance").value(1000))
        assertThat(replicaCount("point_account")).isZero()
        assertThat(replicaCount("point_transaction")).isZero()

        // 커머스 결제 화면·마이 페이지가 바로 읽는다.
        internalGet("/api-internal/point/$user/balance").andExpect(jsonPath("$.balance").value(1000))
        internalGet("/api-internal/point/$user/history")
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].type").value("REFUND"))
    }

    @Test
    fun idempotencyAndLimits_areCheckedOnMaster() {
        val earn = """{"userId":"$user","amount":500,"reason":"PURCHASE","refId":"purchase:order:9"}"""
        internalPost("/api-internal/point/earn-amount", earn).andExpect(jsonPath("$.applied").value(true))
        internalPost("/api-internal/point/earn-amount", earn)
            .andExpect(jsonPath("$.applied").value(false))
            .andExpect(jsonPath("$.reason").value("DUPLICATE"))
            .andExpect(jsonPath("$.balance").value(500))

        val signup = """{"userId":"$user","ruleCode":"SIGNUP"}"""
        internalPost("/api-internal/point/earn", signup).andExpect(jsonPath("$.applied").value(true))
        internalPost("/api-internal/point/earn", signup)
            .andExpect(jsonPath("$.applied").value(false))
            .andExpect(jsonPath("$.reason").value("TOTAL_LIMIT"))

        internalPost("/api-internal/point/spend", """{"userId":"$user","amount":600,"refId":"order:9"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.balance").value(0))
        internalPost("/api-internal/point/spend", """{"userId":"$user","amount":1,"refId":"order:10"}""")
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"))
    }

    @Test
    fun app_checkIn_thenMyBalanceAndHistory_readFresh() {
        mockMvc.perform(post("/api-public/point/me/checkin").header("X-Auth-User-Id", user))
            .andExpect(status().isOk).andExpect(jsonPath("$.applied").value(true))
        mockMvc.perform(post("/api-public/point/me/checkin").header("X-Auth-User-Id", user))
            .andExpect(jsonPath("$.applied").value(false))
        mockMvc.perform(get("/api-public/point/me").header("X-Auth-User-Id", user))
            .andExpect(jsonPath("$.balance").value(10))
        mockMvc.perform(get("/api-public/point/me/history").header("X-Auth-User-Id", user))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].ruleCode").value("DAILY_CHECKIN"))
    }

    @Test
    fun admin_adjust_thenAccountDetailAndHistory_readFresh_butListBrowsesReplica() {
        internalPost("/api-admin/point/accounts/$user/adjust", """{"amount":300,"memo":"이벤트 보상"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.balance").value(300))

        // 백오피스 상세는 조정 직후 계정과 원장을 다시 읽는다(첫 조정이면 계정도 방금 생겼다).
        internalGet("/api-admin/point/accounts/$user").andExpect(status().isOk).andExpect(jsonPath("$.balance").value(300))
        internalGet("/api-admin/point/accounts/$user/history")
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].type").value("ADJUST"))
        // 목록은 둘러보기라 레플리카를 읽는다 — 복제가 따라오기 전에는 새 계정이 안 보인다.
        internalGet("/api-admin/point/accounts").andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun rule_createdOrChanged_appliesToTheNextEarn_whileTheListBrowsesReplica() {
        internalPost("/api-admin/point/rules", """{"code":"LAG_RULE","name":"지연 규칙","points":7}""")
            .andExpect(status().isCreated).andExpect(jsonPath("$.code").value("LAG_RULE"))
        internalPost("/api-admin/point/rules", """{"code":"LAG_RULE","name":"중복","points":1}""")
            .andExpect(status().isConflict)
        internalPost("/api-internal/point/earn", """{"userId":"$user","ruleCode":"LAG_RULE"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.amount").value(7))

        mockMvc.perform(
            put("/api-admin/point/rules/DAILY_CHECKIN").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"출석 체크","points":15,"dailyLimit":1,"enabled":true}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.points").value(15))
        mockMvc.perform(post("/api-public/point/me/checkin").header("X-Auth-User-Id", user))
            .andExpect(jsonPath("$.amount").value(15))

        internalGet("/api-admin/point/rules")
            .andExpect(jsonPath("$[?(@.code=='LAG_RULE')]").isEmpty)
            .andExpect(jsonPath("$[?(@.code=='DAILY_CHECKIN')].points").value(10))
    }
}
