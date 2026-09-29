package com.example.profileservice.api.replica

import com.example.profileservice.api.member.MemberFeignClient
import com.example.profileservice.api.member.MemberIdDto
import com.example.profileservice.api.storage.StorageFeignClient
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 "쓰고 바로 읽는" 흐름이 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 master 를 통째로(스키마+데이터) 복사해 두고, 그 뒤로는 아무것도
 * 복제하지 않는다. 그래서 테스트 안에서 쓴 값은 master 에만 있다. member-service 가 기록을 추가한 바로 뒤에 읽는 내부 조회와
 * 삭제 전 확인은 master 를 읽어야 하고, 거꾸로 앱의 둘러보기는 레플리카를 읽으므로 옛 모습이 보여야 한다.
 */
@SpringBootTest(
    properties = ["spring.datasource.replica.url=jdbc:h2:mem:profile_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE"],
)
@AutoConfigureMockMvc
class ReplicaLagTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient

    private lateinit var master: JdbcTemplate
    private lateinit var replica: JdbcTemplate

    private val token = "test-internal-token"
    private val user = "lag-user-1"
    private val memberId = 501L

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
        clearProfiles()
        // 복제가 멈추기 전에 있던 기록 하나: 레플리카에도 있다.
        master.update("insert into profile (member_id, profile_type, `value`, created_date) values (?, 2, 'old.png', now())", memberId)
        val script = master.queryForList("SCRIPT", String::class.java)
        replica.execute("DROP ALL OBJECTS")
        script.forEach { replica.execute(it) }
        whenever(memberFeignClient.getMember(user)).thenReturn(MemberIdDto(memberId, user))
    }

    @AfterEach
    fun clearProfiles() {
        master.update("delete from profile")
    }

    private fun replicaCount(): Long = requireNotNull(replica.queryForObject("select count(*) from profile", Long::class.java))

    private fun create(path: String, value: String, header: String, headerValue: String): Long {
        val body = mockMvc.perform(
            post(path).header(header, headerValue).contentType(MediaType.APPLICATION_JSON)
                .content("""{"memberId":$memberId,"profileType":"PROFILE_IMAGE","value":"$value"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.id").isNumber).andReturn().response.contentAsString
        return (com.jayway.jsonpath.JsonPath.read<Number>(body, "$.id")).toLong()
    }

    @Test
    fun memberService_addsProfile_thenReadsHistory_fresh() {
        create("/api-internal/profile", "new.png", "X-Internal-Token", token)
        assertThat(replicaCount()).isEqualTo(1L)

        // member-service 는 추가 직후 회원 정보에 붙일 기록 목록을 읽는다.
        mockMvc.perform(get("/api-internal/profile/$memberId").header("X-Internal-Token", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.value=='new.png')]").isNotEmpty)
    }

    @Test
    fun app_createsThenDeletes_theNewRow_checkedOnMaster() {
        val id = create("/api-public/profile", "mine.png", "X-Auth-User-Id", user)

        // 삭제 전 확인이 레플리카를 읽으면 방금 만든 기록을 못 찾아 404 가 된다.
        mockMvc.perform(delete("/api-public/profile/$memberId/$id").header("X-Auth-User-Id", user))
            .andExpect(status().isOk).andExpect(content().string("1"))
        assertThat(master.queryForObject("select count(*) from profile", Long::class.java)).isEqualTo(1L)
    }

    @Test
    fun app_browsing_readsReplica() {
        val id = create("/api-public/profile", "mine.png", "X-Auth-User-Id", user)

        // 둘러보기는 레플리카를 읽는다 — 복제가 따라오기 전에는 새 기록이 안 보인다.
        mockMvc.perform(get("/api-public/profile/$memberId").header("X-Auth-User-Id", user))
            .andExpect(status().isOk).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].value").value("old.png"))
        mockMvc.perform(get("/api-public/profile/total/count/$memberId").header("X-Auth-User-Id", user))
            .andExpect(content().string("1"))
        mockMvc.perform(get("/api-public/profile/latest/$memberId").header("X-Auth-User-Id", user))
            .andExpect(jsonPath("$.value").value("old.png"))
        mockMvc.perform(get("/api-public/profile/$memberId/$id").header("X-Auth-User-Id", user)).andExpect(status().isNotFound)
    }
}
