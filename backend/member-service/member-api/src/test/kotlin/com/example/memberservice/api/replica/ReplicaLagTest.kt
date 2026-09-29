package com.example.memberservice.api.replica

import com.example.memberservice.api.client.chat.ChatFeignClient
import com.example.memberservice.api.client.commerce.CommerceClient
import com.example.memberservice.api.client.profile.ProfileFeignClient
import com.example.memberservice.api.client.push.PushFeignClient
import com.example.memberservice.api.client.storage.StorageFeignClient
import com.example.memberservice.api.support.DbCleaner
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 "쓰고 바로 읽는" 흐름이 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 회원 셋(alice, bob, boss)만 있는 master 를 통째로(스키마+데이터) 복사해 두고,
 * 그 뒤로는 아무것도 복제하지 않는다. 그래서 테스트 안에서 쓴 값은 master 에만 있다 — 쓰기 직후 읽기가 레플리카로 가면
 * 404·빈 목록·옛 값으로 드러난다. 거꾸로 둘러보기(목록·검색)는 레플리카를 읽으므로 옛 모습이 보여야 한다.
 */
@SpringBootTest(
    properties = ["spring.datasource.replica.url=jdbc:h2:mem:member_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1"],
)
@AutoConfigureMockMvc
class ReplicaLagTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var memberRepository: MemberRwRepository
    @MockitoBean lateinit var chatFeignClient: ChatFeignClient
    @MockitoBean lateinit var pushFeignClient: PushFeignClient
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient
    @MockitoBean lateinit var commerceClient: CommerceClient

    private lateinit var master: JdbcTemplate
    private lateinit var replica: JdbcTemplate

    private lateinit var alice: Member
    private lateinit var bob: Member
    private lateinit var boss: Member

    @Autowired
    fun dataSources(
        @Qualifier("rwHikariDataSource") masterDataSource: DataSource,
        @Qualifier("roHikariDataSource") replicaDataSource: DataSource,
    ) {
        master = JdbcTemplate(masterDataSource)
        replica = JdbcTemplate(replicaDataSource)
    }

    private fun member(tag: String) = memberRepository.save(
        Member(userId = "lag-$tag", email = "$tag@lag.test", username = tag, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    @BeforeEach
    fun freezeReplica() {
        DbCleaner.clean(master.dataSource!!)
        alice = member("alice")
        bob = member("bob")
        boss = member("boss")
        val script = master.queryForList("SCRIPT", String::class.java)
        replica.execute("DROP ALL OBJECTS")
        script.forEach { replica.execute(it) }
        whenever(profileFeignClient.getMemberProfiles(anyLong())).thenReturn(ResponseEntity.ok(listOf()))
    }

    @AfterEach
    fun clean() {
        DbCleaner.clean(master.dataSource!!)
    }

    private fun replicaCount(table: String): Long =
        requireNotNull(replica.queryForObject("select count(*) from $table", Long::class.java))

    private fun internal(builder: MockHttpServletRequestBuilder, actor: String? = null, body: String? = null): ResultActions {
        builder.header("X-Internal-Token", "test-internal-token")
        actor?.let { builder.header("X-Auth-User-Id", it) }
        body?.let { builder.contentType(MediaType.APPLICATION_JSON).content(it) }
        return mockMvc.perform(builder)
    }

    private fun app(builder: MockHttpServletRequestBuilder, userId: String, body: String? = null): ResultActions {
        builder.header("X-Auth-User-Id", userId)
        body?.let { builder.contentType(MediaType.APPLICATION_JSON).content(it) }
        return mockMvc.perform(builder)
    }

    /** auth-service 가 회원을 만들고 바로 쓰고, 앱은 토큰을 받자마자 이메일로 자기 정보를 읽는다. */
    @Test
    fun googleSignup_thenLookups_readFresh_butAdminListBrowsesReplica() {
        internal(post("/api-internal/member/google"), body = """{"sub":"lag-new","email":"new@lag.test","name":"새회원","picture":""}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.userId").value("lag-new"))
        assertThat(replicaCount("member")).isEqualTo(3)

        internal(get("/api-internal/member/id/lag-new")).andExpect(status().isOk).andExpect(jsonPath("$.email").value("new@lag.test"))
        internal(get("/api-internal/member/by-email/new@lag.test")).andExpect(status().isOk)
        internal(get("/api-internal/member/lag-new/role")).andExpect(status().isOk)
        app(get("/api-public/member/new@lag.test"), "lag-new").andExpect(status().isOk).andExpect(jsonPath("$.username").value("새회원"))
        // 다시 로그인해도 새로 만들지 않는다(멱등 검사는 master).
        internal(post("/api-internal/member/google"), body = """{"sub":"lag-new","email":"new@lag.test","name":"다른이름","picture":""}""")
            .andExpect(jsonPath("$.username").value("새회원"))
        assertThat(master.queryForObject("select count(*) from member", Long::class.java)).isEqualTo(4)

        // 백오피스 목록·검색은 둘러보기라 레플리카를 읽는다 — 복제가 따라오기 전에는 새 회원이 안 보인다.
        internal(get("/api-admin/member").param("keyword", "lag.test")).andExpect(jsonPath("$.totalElements").value(3))
    }

    /** 차단 직후 ws-service·chat-service·앱이 차단 목록을 읽어 60초 캐시에 담는다. */
    @Test
    fun block_thenBlockedLists_readFresh() {
        app(post("/api-public/member/${alice.userId}/friends"), alice.userId, """{"email":"bob@lag.test"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.userId").value(bob.userId))
        app(put("/api-public/member/${alice.userId}/friends/${bob.id}/blocked"), alice.userId, """{"on":true}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.status").value("BLOCKED"))
        assertThat(replicaCount("member_friend")).isZero()

        internal(get("/api-internal/member/${alice.userId}/blocked-ids")).andExpect(jsonPath("$[0]").value(bob.userId))
        internal(get("/api-internal/member/${bob.userId}/blocked-by")).andExpect(jsonPath("$[0]").value(alice.userId))
        app(get("/api-public/member/${alice.userId}/friends/blocked-ids"), alice.userId).andExpect(jsonPath("$[0]").value(bob.userId))
        app(get("/api-public/member/${alice.userId}/friends/${bob.id}"), alice.userId).andExpect(jsonPath("$.status").value("BLOCKED"))
    }

    /** 앱은 친구 추가·이름 변경·즐겨찾기·숨김·차단 직후에 친구 목록과 이름표를 다시 받는다. 백오피스 친구 탭은 둘러보기다. */
    @Test
    fun friendChanges_thenAppFriendListAndNames_readFresh_butAdminFriendTabBrowsesReplica() {
        fun friends(filter: String) = app(get("/api-public/member/${alice.userId}/friends").param("filter", filter), alice.userId)
        fun names() = app(get("/api-public/member/${alice.userId}/friends/names"), alice.userId)
        fun change(what: String, body: String) =
            app(put("/api-public/member/${alice.userId}/friends/${bob.id}/$what"), alice.userId, body).andExpect(status().isOk)

        // 친구 추가 직후
        app(post("/api-public/member/${alice.userId}/friends"), alice.userId, """{"email":"bob@lag.test"}""").andExpect(status().isOk)
        app(post("/api-public/member/${alice.userId}/friends"), alice.userId, """{"email":"boss@lag.test"}""").andExpect(status().isOk)
        friends("normal")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].userId").value(bob.userId))
            .andExpect(jsonPath("$.content[0].favorite").value(false))
        names().andExpect(jsonPath("$.['${bob.userId}']").value("bob"))
        // 페이지를 잘라도 전체 수는 master 기준이다.
        app(get("/api-public/member/${alice.userId}/friends").param("page", "1").param("size", "1"), alice.userId)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].userId").value(boss.userId))

        // 이름 변경 직후
        change("name", """{"name":"밥형"}""")
        friends("normal").andExpect(jsonPath("$.content[?(@.userId=='${bob.userId}')].friendName").value("밥형"))
        names().andExpect(jsonPath("$.['${bob.userId}']").value("밥형"))

        // 즐겨찾기 직후
        change("favorite", """{"on":true}""")
        friends("favorite").andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].userId").value(bob.userId))

        // 숨김 직후
        change("hidden", """{"on":true}""")
        friends("hidden").andExpect(jsonPath("$.totalElements").value(1))
        friends("normal").andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].userId").value(boss.userId))

        // 차단 직후: 즐겨찾기가 꺼지고 차단 목록으로 옮겨 간다. 이름표에는 그대로 남는다.
        change("blocked", """{"on":true}""")
        friends("blocked")
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("BLOCKED"))
            .andExpect(jsonPath("$.content[0].favorite").value(false))
        friends("favorite").andExpect(jsonPath("$.totalElements").value(0))
        friends("hidden").andExpect(jsonPath("$.totalElements").value(0))
        names().andExpect(jsonPath("$.length()").value(2))

        // 복제는 하나도 오지 않았다. 백오피스 친구 탭·회원 상세는 둘러보기라 레플리카의 옛 모습(친구 없음)을 본다.
        assertThat(replicaCount("member_friend")).isZero()
        internal(get("/api-admin/member/${alice.id}/friends")).andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(0))
        internal(get("/api-admin/member/${alice.id}")).andExpect(jsonPath("$.friendCount").value(0))
        // 이메일로 친구 찾기도 둘러보기다 — 레플리카에 있는 회원은 찾는다.
        app(get("/api-public/member/friends/bob@lag.test"), alice.userId).andExpect(jsonPath("$[0].userId").value(bob.userId))
    }

    /** 콘솔은 권한을 저장한 직후 직원 목록·회원 상세를 다시 읽고, 그 직원은 바로 로그인한다. */
    @Test
    fun staffPermissionSaved_thenLoginAndReload_readFresh_butMemberListBrowsesReplica() {
        internal(put("/api-super/staff/${alice.id}"), actor = boss.userId, body = """{"permissions":["ADMIN","INTERNAL"]}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.permissions[0]").value("ADMIN"))
        assertThat(replicaCount("staff")).isZero()

        internal(get("/api-internal/staff/user/${alice.userId}")).andExpect(status().isOk).andExpect(jsonPath("$.permissions[1]").value("INTERNAL"))
        internal(get("/api-internal/staff/by-email/${alice.email}")).andExpect(status().isOk)
        internal(get("/api-super/staff")).andExpect(jsonPath("$[0].userId").value(alice.userId))
        internal(get("/api-staff/member/${alice.id}")).andExpect(jsonPath("$.staff.permissions[0]").value("ADMIN"))

        internal(get("/api-staff/member").param("staffOnly", "true")).andExpect(jsonPath("$.totalElements").value(0))

        internal(delete("/api-super/staff/${alice.id}"), actor = boss.userId).andExpect(status().isNoContent)
        internal(get("/api-internal/staff/user/${alice.userId}")).andExpect(status().isNotFound)
    }

    /** 백오피스는 공지를 올린 직후·설정을 저장한 직후 목록을 다시 읽는다. 앱은 둘러본다. */
    @Test
    fun noticeAndCommonData_adminReloadsFresh_appBrowsesReplica() {
        internal(post("/api-admin/notice"), actor = boss.userId, body = """{"title":"지연 공지","content":"본문","push":false}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.writer").value("boss"))
        internal(get("/api-admin/notice")).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].title").value("지연 공지"))
        app(get("/api-public/notice"), alice.userId).andExpect(status().isOk).andExpect(jsonPath("$").isEmpty)

        internal(put("/api-admin/common/version"), body = """{"value":"1.2.3"}""").andExpect(status().isOk)
        internal(put("/api-admin/common/version"), body = """{"value":"1.2.4"}""").andExpect(jsonPath("$.value").value("1.2.4"))
        internal(get("/api-admin/common/version")).andExpect(jsonPath("$.value").value("1.2.4"))
        internal(get("/api-admin/common")).andExpect(jsonPath("$[0].value").value("1.2.4"))
        assertThat(master.queryForObject("select count(*) from common_data", Long::class.java)).isEqualTo(1)
        app(get("/api-public/common/version"), alice.userId).andExpect(status().isNotFound)
    }

    /** 이용 기록은 "있으면 고치고 없으면 만든다" — 있는지는 master 로 본다. 방금 가입한 회원의 첫 토큰 발급도 기록된다. */
    @Test
    fun usageRecord_upsertsOnMaster_evenForAMemberCreatedJustNow() {
        internal(post("/api-internal/member/google"), body = """{"sub":"lag-fresh","email":"fresh@lag.test","name":"방금","picture":""}""")
            .andExpect(status().isOk)
        val usage = """{"userId":"lag-fresh","clientId":"modu-chat"}"""
        internal(post("/api-internal/member/usage"), body = usage).andExpect(status().isNoContent)
        internal(post("/api-internal/member/usage"), body = usage).andExpect(status().isNoContent)

        assertThat(master.queryForObject("select count(*) from member_service_usage where user_id = 'lag-fresh'", Long::class.java)).isEqualTo(1)
        assertThat(replicaCount("member_service_usage")).isZero()
    }

    /** 내 정보는 고친 직후에 다시 읽는다. 앱의 프로필 수정 응답도 방금 값이다. */
    @Test
    fun profileUpdates_readFresh() {
        internal(put("/api-admin/member/me"), actor = boss.userId, body = """{"username":"대표"}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.member.username").value("대표"))
        internal(get("/api-admin/member/me"), actor = boss.userId).andExpect(jsonPath("$.member.username").value("대표"))

        app(post("/api-public/member/${alice.userId}"), alice.userId, """{"username":"앨리스","statusMessage":"안녕","profileImage":"","wallpaperImage":""}""")
            .andExpect(status().isOk).andExpect(jsonPath("$.username").value("앨리스"))
        app(get("/api-public/member/${alice.email}"), alice.userId).andExpect(jsonPath("$.statusMessage").value("안녕"))
        // 남의 프로필 보기는 둘러보기라 레플리카의 옛 값이다.
        app(get("/api-public/member/member/${alice.id}"), bob.userId).andExpect(jsonPath("$.username").value("alice"))
    }

    /** 탈퇴 직후 다시 부르면 아무것도 하지 않고(멱등), 같은 구글 계정으로 로그인하면 같은 행이 되살아난다. */
    @Test
    fun withdraw_thenWithdrawAgain_thenSignIn_allSeeTheLatestState() {
        app(delete("/api-public/member/${alice.userId}"), alice.userId).andExpect(status().isNoContent)
        app(delete("/api-public/member/${alice.userId}"), alice.userId).andExpect(status().isNoContent)
        org.mockito.kotlin.verify(chatFeignClient, org.mockito.kotlin.times(1)).exitAllChatRooms(alice.id!!)
        internal(get("/api-internal/member/id/${alice.userId}")).andExpect(jsonPath("$.username").value(Member.WITHDRAWN_USERNAME))

        internal(post("/api-internal/member/google"), body = """{"sub":"${alice.userId}","email":"alice@lag.test","name":"돌아온 앨리스","picture":""}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(alice.id))
            .andExpect(jsonPath("$.username").value("돌아온 앨리스"))
        assertThat(replica.queryForObject("select username from member where user_id = ?", String::class.java, alice.userId)).isEqualTo("alice")
    }
}
