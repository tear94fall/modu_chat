package com.example.chatservice.api.replica

import com.example.chatservice.api.member.MemberFeignClient
import com.example.chatservice.api.support.ChatTables
import com.example.chatservice.api.support.stubMembers
import com.example.chatservice.application.event.ChatEventPublisher
import com.jayway.jsonpath.JsonPath
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 "쓰고 바로 읽는" 흐름이 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 master(방 하나, 메시지 둘)를 통째로 복사해 두고, 그 뒤로는
 * 아무것도 복제하지 않는다. 그래서 테스트 안에서 쓴 값은 master 에만 있다 — 방 조회·방 목록·최근 메시지·읽음 커서가
 * 레플리카를 읽으면 없는 방·빠진 메시지·되살아난 배지로 드러난다. 거꾸로 둘러보기(지난 대화, 백오피스)는
 * 레플리카를 읽으므로 옛 모습이 보여야 한다.
 */
@SpringBootTest(
    properties = ["spring.datasource.replica.url=jdbc:h2:mem:chat_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1"],
)
@AutoConfigureMockMvc
class ReplicaLagTest {

    companion object {
        private const val OLD_ROOM = "room-old"
        private const val TOKEN = "test-internal-token"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private lateinit var master: JdbcTemplate
    private lateinit var replica: JdbcTemplate
    private lateinit var tables: ChatTables
    private var oldChat = 0L

    @Autowired
    fun dataSources(
        @Qualifier("rwHikariDataSource") masterDataSource: DataSource,
        @Qualifier("roHikariDataSource") replicaDataSource: DataSource,
    ) {
        master = JdbcTemplate(masterDataSource)
        replica = JdbcTemplate(replicaDataSource)
        tables = ChatTables(master)
    }

    @BeforeEach
    fun freezeReplica() {
        memberFeignClient.stubMembers()
        tables.clear()
        tables.room(OLD_ROOM, "오래된 방", "", 1, 2)
        tables.chat(OLD_ROOM, "user-2", "오래된 메시지 1", 1, "2026-09-20 09:00:01")
        oldChat = tables.chat(OLD_ROOM, "user-2", "오래된 메시지 2", 1, "2026-09-20 09:00:02")
        tables.setLastChat(OLD_ROOM, oldChat)

        val script = master.queryForList("SCRIPT", String::class.java)
        replica.execute("DROP ALL OBJECTS")
        script.forEach { replica.execute(it) }
    }

    @AfterEach
    fun clear() = tables.clear()

    private fun replicaCount(table: String): Long =
        requireNotNull(replica.queryForObject("select count(*) from $table", Long::class.java))

    private fun app(builder: MockHttpServletRequestBuilder, user: Long = 1): ResultActions =
        mockMvc.perform(builder.header("X-Auth-User-Id", "user-$user"))

    private fun internal(builder: MockHttpServletRequestBuilder): ResultActions =
        mockMvc.perform(builder.header("X-Internal-Token", TOKEN))

    private fun json(builder: MockHttpServletRequestBuilder, body: String) =
        builder.contentType(MediaType.APPLICATION_JSON).content(body)

    private fun saveChat(roomId: String, sender: String, message: String): Long =
        internal(json(post("/api-internal/chat"), """{"chatType":1,"roomId":"$roomId","sender":"$sender","message":"$message","chatTime":"2026-09-20 11:00:00"}"""))
            .andExpect(status().isOk).andReturn().response.contentAsString.toLong()

    @Test
    fun createRoom_thenAppAndWsService_readItRightAway() {
        val created = app(json(post("/api-public/chat/chat/room"), "[1,3]"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val roomId = JsonPath.read<String>(created, "$.roomId")
        assertThat(replicaCount("chat_room")).isEqualTo(1)

        // ws-service 는 방 생성 이벤트를 받자마자, 앱은 응답을 받자마자 방과 방 목록을 읽는다.
        internal(get("/api-internal/chat/$roomId/room")).andExpect(status().isOk).andExpect(jsonPath("$.members.length()").value(2))
        app(get("/api-public/chat/$roomId/room"), user = 3).andExpect(status().isOk).andExpect(jsonPath("$.roomId").value(roomId))
        app(get("/api-public/chat/1/rooms")).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/3/rooms"), user = 3).andExpect(jsonPath("$[0].roomId").value(roomId))
        app(get("/api-public/chat/unread/3"), user = 3).andExpect(jsonPath("$[0].roomId").value(roomId))
        app(json(post("/api-public/chat/room/user-1").contentType(MediaType.TEXT_PLAIN), "user-3"))
            .andExpect(jsonPath("$[0].roomId").value(roomId))
        // 같은 멤버로 다시 만들면 방금 만든 방을 돌려준다(중복 검사도 master).
        app(json(post("/api-public/chat/chat/room"), "[3,1]")).andExpect(jsonPath("$.roomId").value(roomId))
        // 방금 만든 방에 바로 메시지를 저장하고 읽는다.
        val chatId = saveChat(roomId, "user-1", "첫 메시지")
        app(get("/api-public/chat/$roomId/page/30"), user = 3).andExpect(jsonPath("$[0].id").value(chatId))
    }

    @Test
    fun invite_thenMemberList_andLeave_thenRoomList_readFresh() {
        app(json(post("/api-public/chat/$OLD_ROOM/member"), """["user-3"]""")).andExpect(status().isOk)
        assertThat(replicaCount("chat_room_member")).isEqualTo(2)

        // 초대 직후의 멤버 목록·읽음 커서, 초대받은 사람의 방 입장(멤버 확인도 master).
        app(get("/api-public/chat/$OLD_ROOM/room")).andExpect(jsonPath("$.members.length()").value(3))
        app(get("/api-public/chat/read/$OLD_ROOM")).andExpect(jsonPath("$.length()").value(3))
        app(get("/api-public/chat/$OLD_ROOM/page/30"), user = 3).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/$OLD_ROOM/chats"), user = 3).andExpect(status().isOk)

        app(delete("/api-public/chat/$OLD_ROOM/member/user-1")).andExpect(status().isOk)
        app(get("/api-public/chat/1/rooms")).andExpect(jsonPath("$.length()").value(0))
        app(get("/api-public/chat/$OLD_ROOM/chats")).andExpect(status().isForbidden)
        app(get("/api-public/chat/$OLD_ROOM/room"), user = 2).andExpect(jsonPath("$.members.length()").value(2))
    }

    @Test
    fun saveChat_thenWsServiceAndApp_readIt_andUnreadFollowsTheReadMarker() {
        val chatId = saveChat(OLD_ROOM, "user-2", "방금 보낸 메시지")
        internal(json(post("/api-internal/chat/$OLD_ROOM/room"), """{"roomName":"오래된 방","roomImage":"","lastChatMsg":"방금 보낸 메시지","lastChatId":"$chatId","lastChatTime":"2026-09-20 11:00:00"}"""))
            .andExpect(status().isOk)
        assertThat(replicaCount("chat")).isEqualTo(2)

        // ws-service 가 브로드캐스트 전에, 앱이 알림을 받고 읽는다.
        internal(get("/api-internal/chat/$chatId")).andExpect(status().isOk).andExpect(jsonPath("$.message").value("방금 보낸 메시지"))
        app(get("/api-public/chat/$chatId")).andExpect(jsonPath("$.id").value(chatId))
        app(get("/api-public/chat").param("ids", chatId.toString())).andExpect(jsonPath("$[0].id").value(chatId))
        app(get("/api-public/chat/$OLD_ROOM/$chatId")).andExpect(jsonPath("$.id").value(chatId))
        app(get("/api-public/chat/$OLD_ROOM/page/30")).andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].id").value(chatId))
        app(get("/api-public/chat/$OLD_ROOM/room")).andExpect(jsonPath("$.lastChatId").value(chatId.toString()))

        // 읽음 처리 직후의 안 읽은 개수와 읽음 커서.
        app(get("/api-public/chat/unread/1")).andExpect(jsonPath("$[0].unreadChatCount").value(3))
        app(post("/api-public/chat/read/$OLD_ROOM/1")).andExpect(status().isNoContent)
        app(get("/api-public/chat/unread/1"))
            .andExpect(jsonPath("$[0].unreadChatCount").value(0))
            .andExpect(jsonPath("$[0].lastReadChatId").value(chatId))
        internal(post("/api-internal/chat/read/$OLD_ROOM/user-2")).andExpect(status().isNoContent)
        app(get("/api-public/chat/read/$OLD_ROOM"))
            .andExpect(jsonPath("$[?(@.userId=='user-1')].lastReadChatId").value(chatId.toInt()))
            .andExpect(jsonPath("$[?(@.userId=='user-2')].lastReadChatId").value(chatId.toInt()))
    }

    @Test
    fun reaction_thenMessageFetch_showsIt_andDeleteChecksMaster() {
        val reaction = json(post("/api-internal/chat/reaction/$OLD_ROOM/$oldChat/user-1"), """{"emoji":"HEART"}""")
        internal(reaction).andExpect(status().isOk).andExpect(jsonPath("$.added").value(true))
        assertThat(replicaCount("chat_reaction")).isZero()

        app(get("/api-public/chat/$oldChat")).andExpect(jsonPath("$.reactions[0].emoji").value("HEART"))
        app(get("/api-public/chat").param("ids", oldChat.toString())).andExpect(jsonPath("$[0].reactions[0].userIds[0]").value("user-1"))
        app(get("/api-public/chat/$OLD_ROOM/page/30")).andExpect(jsonPath("$[0].reactions[0].emoji").value("HEART"))
        // 이미 남긴 반응인지도 master 로 본다: 같은 이모지를 다시 보내면 취소다.
        internal(json(post("/api-internal/chat/reaction/$OLD_ROOM/$oldChat/user-1"), """{"emoji":"HEART"}"""))
            .andExpect(jsonPath("$.added").value(false))

        val mine = saveChat(OLD_ROOM, "user-1", "지울 메시지")
        app(delete("/api-public/chat/$OLD_ROOM/$mine"), user = 2).andExpect(status().isForbidden)
        app(delete("/api-public/chat/$OLD_ROOM/$mine")).andExpect(status().isOk)
        app(get("/api-public/chat/$mine")).andExpect(status().isNotFound)
    }

    @Test
    fun browsing_readsTheReplica() {
        saveChat(OLD_ROOM, "user-2", "복제 전의 메시지")
        app(json(post("/api-public/chat/chat/room"), "[1,3]")).andExpect(status().isOk)

        // 지난 대화 둘러보기는 레플리카를 읽는다 — 복제가 따라오기 전에는 새 메시지가 안 보인다.
        app(get("/api-public/chat/$OLD_ROOM/chats")).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/$OLD_ROOM/page")).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/$OLD_ROOM/${Long.MAX_VALUE}/30")).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/$OLD_ROOM/chat").param("message", "메시지")).andExpect(jsonPath("$.length()").value(2))
        app(get("/api-public/chat/$OLD_ROOM/count")).andExpect(jsonPath("$").value(2))
        // 백오피스 목록·상세·대화도 레플리카다.
        internal(get("/api-admin/chat/rooms")).andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(1))
        internal(get("/api-admin/chat/rooms/$OLD_ROOM")).andExpect(status().isOk).andExpect(jsonPath("$.lastChatId").value(oldChat.toString()))
        internal(get("/api-admin/chat/rooms/$OLD_ROOM/chats")).andExpect(jsonPath("$.totalElements").value(2))
        // 최근 메시지(방 입장)는 master 다.
        app(get("/api-public/chat/$OLD_ROOM/page/30")).andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun withdrawal_cleansUpOnMaster() {
        internal(delete("/api-internal/chat/member/1/rooms")).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(1))
        internal(delete("/api-internal/chat/member/2/rooms")).andExpect(status().isOk)

        internal(get("/api-internal/chat/$OLD_ROOM/room")).andExpect(status().isNotFound)
        assertThat(tables.count("chat")).isZero()
        assertThat(replicaCount("chat_room")).isEqualTo(1)
    }
}
