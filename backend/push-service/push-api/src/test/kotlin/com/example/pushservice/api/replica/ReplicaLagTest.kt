package com.example.pushservice.api.replica

import com.example.pushservice.application.push.PushSender
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 토큰 쓰기가 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 master 를 통째로(스키마+데이터) 복사해 두고, 그 뒤로는
 * 아무것도 복제하지 않는다. 토큰 upsert 의 "이미 있나" 검사가 레플리카를 읽으면 같은 회원의 행이 또 생긴다.
 * 거꾸로 발송 대상 조회는 레플리카를 읽으므로 복제 전에는 옛 모습(복사 시점의 토큰)이 보여야 한다.
 */
@SpringBootTest(
    properties = ["spring.datasource.replica.url=jdbc:h2:mem:push_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1"],
)
@AutoConfigureMockMvc
class ReplicaLagTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var pushSender: PushSender

    private lateinit var master: JdbcTemplate
    private lateinit var replica: JdbcTemplate

    private val token = "test-internal-token"

    @Autowired
    fun dataSources(
        @Qualifier("rwHikariDataSource") masterDataSource: DataSource,
        @Qualifier("roHikariDataSource") replicaDataSource: DataSource,
    ) {
        master = JdbcTemplate(masterDataSource)
        replica = JdbcTemplate(replicaDataSource)
    }

    /** 복사 시점: old-user 의 토큰 하나만 있다. */
    @BeforeEach
    fun freezeReplica() {
        clearTokens()
        master.update("insert into fcm_token (user_id, fcm_token) values ('old-user', 'old-token')")
        val script = master.queryForList("SCRIPT", String::class.java)
        replica.execute("DROP ALL OBJECTS")
        script.forEach { replica.execute(it) }
    }

    @AfterEach
    fun clearTokens() {
        master.update("delete from fcm_token")
    }

    private fun masterTokens(userId: String): List<String> =
        master.queryForList("select fcm_token from fcm_token where user_id = ? order by token_id", String::class.java, userId)

    private fun replicaCount(): Long = requireNotNull(replica.queryForObject("select count(*) from fcm_token", Long::class.java))

    private fun register(userId: String, fcmToken: String): ResultActions =
        mockMvc.perform(
            put("/api-public/push/$userId/token").header("X-Auth-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON).content("\"$fcmToken\""),
        )

    private fun internalPost(path: String, body: String): ResultActions =
        mockMvc.perform(post(path).header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))

    @Test
    fun registerTwice_keepsOneRow_becauseTheExistenceCheckReadsMaster() {
        register("lag-user", "tok-1").andExpect(status().isOk).andExpect(content().string("tok-1"))
        register("lag-user", "tok-2").andExpect(status().isOk).andExpect(content().string("tok-2"))

        assertThat(masterTokens("lag-user")).containsExactly("tok-2")
        assertThat(replicaCount()).isEqualTo(1)
    }

    @Test
    fun refreshingAnExistingToken_updatesTheMasterRow() {
        register("old-user", "new-token").andExpect(status().isOk).andExpect(content().string("new-token"))

        assertThat(masterTokens("old-user")).containsExactly("new-token")
    }

    @Test
    fun withdraw_deletesOnMaster() {
        register("lag-user", "tok-1").andExpect(status().isOk)
        mockMvc.perform(delete("/api-internal/push/token/lag-user").header("X-Internal-Token", token)).andExpect(status().isNoContent)
        mockMvc.perform(delete("/api-internal/push/token/old-user").header("X-Internal-Token", token)).andExpect(status().isNoContent)

        assertThat(masterTokens("lag-user")).isEmpty()
        assertThat(masterTokens("old-user")).isEmpty()
    }

    /** 발송 대상은 레플리카에서 읽는다 — 복제가 따라오기 전에는 방금 등록한 토큰이 아직 안 보인다(그 푸시는 빠진다). */
    @Test
    fun sendTargets_browseReplica() {
        register("lag-user", "tok-1").andExpect(status().isOk)

        internalPost("/api-internal/push/user", """{"userId":"lag-user","title":"t","body":"b"}""").andExpect(status().isNoContent)
        verify(pushSender, never()).sendToToken(anyOrNull(), anyOrNull(), any())

        internalPost("/api-internal/push/user", """{"userId":"old-user","title":"t","body":"b"}""").andExpect(status().isOk)
        verify(pushSender).sendToToken(eq("old-token"), isNull(), any())

        internalPost("/api-admin/push/broadcast", """{"title":"t","body":"b"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.groups").value(1))
        verify(pushSender).sendToTokens(eq(listOf("old-token")), any(), any())
    }
}
